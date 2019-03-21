package com.mediatoolkit.pareco.session;

import com.mediatoolkit.pareco.components.DirectoryStructureReader;
import com.mediatoolkit.pareco.components.DirectoryWriter;
import com.mediatoolkit.pareco.components.FileChunkWriter;
import com.mediatoolkit.pareco.components.FileDeleter;
import com.mediatoolkit.pareco.components.FileDigestCalculator;
import com.mediatoolkit.pareco.components.MetadataWriter;
import com.mediatoolkit.pareco.components.RandomAccessFilePool;
import com.mediatoolkit.pareco.components.RandomAccessFilePool.Mode;
import com.mediatoolkit.pareco.exceptions.AlreadyCommitedException;
import com.mediatoolkit.pareco.exceptions.DuplicateFileMetadataException;
import com.mediatoolkit.pareco.exceptions.FileNotSpecifiedDuringInitializationException;
import com.mediatoolkit.pareco.exceptions.SessionNotExistsException;
import com.mediatoolkit.pareco.model.ChunkInfo;
import com.mediatoolkit.pareco.model.DigestType;
import com.mediatoolkit.pareco.model.DirectoryStructure;
import com.mediatoolkit.pareco.model.FileDigest;
import com.mediatoolkit.pareco.model.FileMetadata;
import com.mediatoolkit.pareco.model.FilePath;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.Synchronized;
import one.util.streamex.StreamEx;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 25/10/2018
 */
public class UploadSession {

	private final DirectoryStructureReader directoryStructureReader;
	private final DirectoryWriter directoryWriter;
	private final FileDeleter fileDeleter;
	private final FileDigestCalculator fileDigestCalculator;
	private final FileChunkWriter fileChunkWriter;
	private final MetadataWriter metadataWriter;

	private final String id;
	@Getter
	private final String rootDirectory;
	private final DirectoryStructure directoryStructure;
	private final long chunkSizeBytes;
	private final String include;
	private final String exclude;

	private final Map<FilePath, FileMetadata> srcFileMetadatas;
	private final Map<String, FileUploadSession> fileSessions;
	private boolean committed;
	@Getter
	private long lastActivityTime;

	public UploadSession(
		DirectoryStructureReader directoryStructureReader,
		DirectoryWriter directoryWriter,
		FileDeleter fileDeleter,
		FileDigestCalculator fileDigestCalculator,
		FileChunkWriter fileChunkWriter,
		MetadataWriter metadataWriter,
		String id, String rootDirectory,
		DirectoryStructure directoryStructure,
		long chunkSizeBytes,
		String include,
		String exclude
	) {
		this.directoryStructureReader = directoryStructureReader;
		this.directoryWriter = directoryWriter;
		this.fileDeleter = fileDeleter;
		this.fileDigestCalculator = fileDigestCalculator;
		this.fileChunkWriter = fileChunkWriter;
		this.metadataWriter = metadataWriter;
		this.id = id;
		this.rootDirectory = rootDirectory;
		this.directoryStructure = directoryStructure;
		this.chunkSizeBytes = chunkSizeBytes;
		this.include = include;
		this.exclude = exclude;
		this.committed = false;
		this.fileSessions = new HashMap<>();
		this.srcFileMetadatas = StreamEx.of(directoryStructure.getFiles())
			.mapToEntry(FileMetadata::getFilePath)
			.invert()
			.toMap((fm1, fm2) -> {
				throw new DuplicateFileMetadataException(
					"Got duplicate file metadata in directory structure for file: " + fm1.getFilePath()
				);
			});
		this.lastActivityTime = System.currentTimeMillis();
	}

	@Synchronized
	public void createRequiredDirectories() throws IOException {
		this.lastActivityTime = System.currentTimeMillis();
		directoryWriter.createDirectories(
			rootDirectory, directoryStructure.getDirectories()
		);
	}

	public DirectoryStructure getCurrentDirectoryStructure() throws IOException {
		this.lastActivityTime = System.currentTimeMillis();
		return directoryStructureReader.readDirectoryStructure(rootDirectory, include, exclude);
	}

	public FileDigest getFileDigest(String relativeDirectory, String fileName, DigestType digestType) throws IOException {
		this.lastActivityTime = System.currentTimeMillis();
		FilePath filePath = FilePath.of(relativeDirectory, fileName);
		checkFileRegistered(filePath);
		return fileDigestCalculator.calculateFileDigest(rootDirectory, filePath, chunkSizeBytes, digestType);
	}

	private void checkFileRegistered(FilePath filePath) {
		this.lastActivityTime = System.currentTimeMillis();
		FileMetadata srcFileMetadata = srcFileMetadatas.get(filePath);
		if (srcFileMetadata == null) {
			throw new FileNotSpecifiedDuringInitializationException("Unspecified file: " + filePath);
		}
	}

	@Synchronized
	public String initFileUploadSession(String relativeDirectory, String fileName) throws IOException {
		this.lastActivityTime = System.currentTimeMillis();
		FilePath filePath = FilePath.of(relativeDirectory, fileName);
		checkFileRegistered(filePath);
		FileMetadata srcFileMetadata = srcFileMetadatas.get(filePath);
		String fileSessionId = id + "_" + UUID.randomUUID().toString();
		FileUploadSession fileUploadSession = new FileUploadSession(rootDirectory, srcFileMetadata);
		fileUploadSession.allocateFileToRequiredSize();
		fileSessions.put(fileSessionId, fileUploadSession);
		return fileSessionId;
	}

	@Synchronized
	public void skipFileUpload(String relativeDirectory, String fileName) {
		this.lastActivityTime = System.currentTimeMillis();
		FilePath filePath = FilePath.of(relativeDirectory, fileName);
		checkFileRegistered(filePath);
	}

	private void checkSessionCommitted() {
		if (committed) {
			throw new AlreadyCommitedException("Session already committed");
		}
	}

	@Synchronized
	public void commit() {
		this.lastActivityTime = System.currentTimeMillis();
		checkSessionCommitted();
		committed = true;
	}

	@Synchronized
	public FileUploadSession getFileUploadSession(String fileSessionId) {
		this.lastActivityTime = System.currentTimeMillis();
		FileUploadSession session = fileSessions.get(fileSessionId);
		if (session == null) {
			throw new SessionNotExistsException("File upload session id: " + fileSessionId);
		}
		return session;
	}

	public void deleteFiles(List<FilePath> filePaths) throws IOException {
		this.lastActivityTime = System.currentTimeMillis();
		fileDeleter.delete(rootDirectory, filePaths);
	}

	@Synchronized
	public void abort() {
		this.lastActivityTime = System.currentTimeMillis();
		committed = true;
		fileSessions.values().forEach(FileUploadSession::abort);
	}

	public class FileUploadSession {

		private final String rootDirectory;
		private final FileMetadata srcFileMetadata;
		private final RandomAccessFilePool randomAccessFilePool;
		private final File file;
		private volatile boolean committed;
		private volatile boolean deleted;

		FileUploadSession(String rootDirectory, FileMetadata srcFileMetadata) {
			this.rootDirectory = rootDirectory;
			this.srcFileMetadata = srcFileMetadata;
			this.file = new File(srcFileMetadata.getFilePath().toAbsolutePath(rootDirectory));
			this.randomAccessFilePool = new RandomAccessFilePool(file, Mode.READ_WRITE);
			committed = false;
			deleted = false;
		}

		private void allocateFileToRequiredSize() throws IOException {
			randomAccessFilePool.doOnFile(file -> fileChunkWriter.allocateFileToSize(
				file, srcFileMetadata.getFileSizeBytes()
			));
		}

		public void uploadChunk(
			long offsetBytes, long sizeBytes, InputStream inputStream
		) throws IOException {
			checkFileCommitted();
			ChunkInfo chunkInfo = ChunkInfo.of(offsetBytes, sizeBytes);
			randomAccessFilePool.doOnFile(file -> fileChunkWriter.writeChunk(
				file, chunkInfo, inputStream
			));
		}

		private void checkFileCommitted() {
			if (committed) {
				throw new AlreadyCommitedException("File: " + srcFileMetadata.getFilePath());
			}
		}

		@SneakyThrows
		public void commit() {
			checkFileCommitted();
			committed = true;
			randomAccessFilePool.close();
			if (!deleted) {
				metadataWriter.writeFileMetadata(rootDirectory, srcFileMetadata);
			}
		}

		public void delete() {
			checkFileCommitted();
			file.delete();
			deleted = true;
		}

		private void abort() {
			randomAccessFilePool.forceClose();
		}

	}

}
