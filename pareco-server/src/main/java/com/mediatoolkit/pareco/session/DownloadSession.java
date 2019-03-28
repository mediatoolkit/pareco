package com.mediatoolkit.pareco.session;

import com.mediatoolkit.pareco.components.DirectoryStructureReader;
import com.mediatoolkit.pareco.components.FileChunkInputStream;
import com.mediatoolkit.pareco.components.FileDigestCalculator;
import com.mediatoolkit.pareco.components.RandomAccessFilePool;
import com.mediatoolkit.pareco.components.RandomAccessFilePool.Mode;
import com.mediatoolkit.pareco.exceptions.AlreadyCommitedException;
import com.mediatoolkit.pareco.exceptions.DuplicateFileMetadataException;
import com.mediatoolkit.pareco.exceptions.FileDeletedException;
import com.mediatoolkit.pareco.exceptions.FileNotSpecifiedDuringInitializationException;
import com.mediatoolkit.pareco.exceptions.SessionNotExistsException;
import com.mediatoolkit.pareco.model.DigestType;
import com.mediatoolkit.pareco.model.DirectoryStructure;
import com.mediatoolkit.pareco.model.FileDigest;
import com.mediatoolkit.pareco.model.FileMetadata;
import com.mediatoolkit.pareco.model.FilePath;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.Synchronized;
import one.util.streamex.StreamEx;
import org.apache.commons.io.IOUtils;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 25/10/2018
 */
public class DownloadSession {

	private final FileDigestCalculator fileDigestCalculator;

	private final String id;
	@Getter
	private final String rootDirectory;
	@Getter
	private final DirectoryStructure directoryStructure;
	private final long chunkSizeBytes;

	private final Map<FilePath, FileMetadata> fileMetadatas;
	private final Map<String, FileDownloadSession> fileSessions;
	private boolean committed;
	@Getter
	private long lastActivityTime;

	public DownloadSession(
		DirectoryStructureReader directoryStructureReader,
		FileDigestCalculator fileDigestCalculator,
		String id, String rootDirectory, long chunkSizeBytes,
		String include, String exclude
	) throws IOException {
		this.fileDigestCalculator = fileDigestCalculator;
		this.id = id;
		this.rootDirectory = rootDirectory;
		this.directoryStructure = directoryStructureReader.readDirectoryStructure(rootDirectory, include, exclude);
		this.chunkSizeBytes = chunkSizeBytes;
		this.committed = false;
		this.fileSessions = new HashMap<>();
		this.fileMetadatas = StreamEx.of(directoryStructure.getFiles())
			.mapToEntry(FileMetadata::getFilePath)
			.invert()
			.toMap((fm1, fm2) -> {
				throw new DuplicateFileMetadataException(
					"Got duplicate file metadata in directory structure for file: " + fm1.getFilePath()
				);
			});
		this.lastActivityTime = System.currentTimeMillis();
	}

	public FileDigest getFileDigest(String relativeDirectory, String fileName, DigestType digestType) throws IOException {
		this.lastActivityTime = System.currentTimeMillis();
		FilePath filePath = FilePath.of(relativeDirectory, fileName);
		checkFileRegistered(filePath);
		return fileDigestCalculator.calculateFileDigest(rootDirectory, filePath, chunkSizeBytes, digestType);
	}

	private void checkFileRegistered(FilePath filePath) {
		FileMetadata fileMetadata = fileMetadatas.get(filePath);
		if (fileMetadata == null) {
			throw new FileNotSpecifiedDuringInitializationException("Unspecified file: " + filePath);
		}
	}

	@Synchronized
	public String initFileDownloadSession(String relativeDirectory, String fileName) throws IOException {
		this.lastActivityTime = System.currentTimeMillis();
		FilePath filePath = FilePath.of(relativeDirectory, fileName);
		checkFileRegistered(filePath);
		FileMetadata fileMetadata = fileMetadatas.get(filePath);
		String fileSessionId = id + "_" + UUID.randomUUID().toString();
		FileDownloadSession fileDownloadSession = new FileDownloadSession(rootDirectory, fileMetadata);
		fileSessions.put(fileSessionId, fileDownloadSession);
		return fileSessionId;
	}

	@Synchronized
	public void skipFileDownload(String relativeDirectory, String fileName) {
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
	public FileDownloadSession getFileDownloadSession(String fileSessionId) {
		this.lastActivityTime = System.currentTimeMillis();
		FileDownloadSession session = fileSessions.get(fileSessionId);
		if (session == null) {
			throw new SessionNotExistsException("File download session id: " + fileSessionId);
		}
		return session;
	}

	@Synchronized
	public void abort() {
		committed = true;
		fileSessions.values().forEach(FileDownloadSession::abort);
	}

	public class FileDownloadSession {

		private final FileMetadata fileMetadata;
		private final RandomAccessFilePool randomAccessFilePool;
		private volatile boolean committed;

		FileDownloadSession(String rootDirectory, FileMetadata fileMetadata) {
			this.fileMetadata = fileMetadata;
			File file = new File(fileMetadata.getFilePath().toAbsolutePath(rootDirectory));
			randomAccessFilePool = new RandomAccessFilePool(file, Mode.READ_ONLY);
			committed = false;
		}

		public void downloadChunk(
			long offsetBytes, long sizeBytes, OutputStream outputStream
		) throws IOException {
			checkFileCommitted();
			try {
				randomAccessFilePool.doOnFile(file -> {
					FileChunkInputStream inputStream = new FileChunkInputStream(file, offsetBytes, sizeBytes);
					IOUtils.copy(inputStream, outputStream);
				});
			} catch (FileNotFoundException ex) {
				throw new FileDeletedException(fileMetadata.getFilePath(), "Can't download chunk on deleted file", ex);
			}
		}

		private void checkFileCommitted() {
			if (committed) {
				throw new AlreadyCommitedException("File: " + fileMetadata.getFilePath());
			}
		}

		@SneakyThrows
		public void commit() {
			checkFileCommitted();
			committed = true;
			randomAccessFilePool.close();
		}

		private void abort() {
			randomAccessFilePool.forceClose();
		}

	}

}
