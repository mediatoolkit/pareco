package com.mediatoolkit.pareco.transfer.upload;

import com.mediatoolkit.pareco.components.ChunkInfosGenerator;
import com.mediatoolkit.pareco.components.DirectoryStructureReader;
import com.mediatoolkit.pareco.components.FileChunkInputStream;
import com.mediatoolkit.pareco.components.ProgressObservableInputStream;
import com.mediatoolkit.pareco.components.RandomAccessFilePool;
import com.mediatoolkit.pareco.components.RandomAccessFilePool.Mode;
import com.mediatoolkit.pareco.components.RandomAccessFilePool.ReturnableRandomAccessFile;
import com.mediatoolkit.pareco.components.TransferNamesEncoding;
import com.mediatoolkit.pareco.model.ChunkInfo;
import com.mediatoolkit.pareco.model.DirectoryStructure;
import com.mediatoolkit.pareco.model.FileMetadata;
import com.mediatoolkit.pareco.model.FilePath;
import com.mediatoolkit.pareco.model.FileStatus;
import com.mediatoolkit.pareco.progress.TransferProgressListener;
import com.mediatoolkit.pareco.restclient.UploadClient;
import com.mediatoolkit.pareco.restclient.UploadClient.FileUploadSessionClient;
import com.mediatoolkit.pareco.restclient.UploadClient.UploadSessionClient;
import com.mediatoolkit.pareco.transfer.ExitTransferAborter;
import com.mediatoolkit.pareco.transfer.FileSizeClassifier;
import com.mediatoolkit.pareco.transfer.FileTransferFilter;
import com.mediatoolkit.pareco.transfer.UnexpectedFilesDeleter;
import com.mediatoolkit.pareco.transfer.model.FileFilterResult;
import com.mediatoolkit.pareco.transfer.model.ServerInfo;
import com.mediatoolkit.pareco.transfer.model.SizeClassifiedFiles;
import com.mediatoolkit.pareco.transfer.model.TransferOptions;
import com.mediatoolkit.pareco.transfer.model.TransferTask;
import static com.mediatoolkit.pareco.util.Util.runIgnoreException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import lombok.AllArgsConstructor;
import one.util.streamex.StreamEx;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 28/10/2018
 */
@Component
@AllArgsConstructor
public class UploadTransferExecutor {

	private final ThreadFactory chunkThreadFactory = new CustomizableThreadFactory("chunk_uploader_");
	private final ThreadFactory fileThreadFactory = new CustomizableThreadFactory("file_uploader_");

	private final UnexpectedFilesDeleter unexpectedFilesDeleter;
	private final FileSizeClassifier fileSizeClassifier;
	private final FileTransferFilter fileTransferFilter;
	private final DirectoryStructureReader directoryStructureReader;
	private final ChunkInfosGenerator chunkInfosGenerator;
	private final ExitTransferAborter exitTransferAborter;
	private final TransferNamesEncoding encoding;

	public void executeUpload(
		TransferTask transferTask,
		TransferProgressListener progressListener
	) throws IOException {
		ServerInfo serverInfo = transferTask.getServerInfo();
		UploadClient uploadClient = UploadClient.builder()
			.httpScheme(serverInfo.getHttpScheme())
			.host(serverInfo.getHost())
			.port(serverInfo.getPort())
			.timeout(transferTask.getOptions().getTimeout())
			.authToken(transferTask.getAuthToken())
			.encoding(encoding)
			.build();
		progressListener.initializing(
			"upload", transferTask.getLocalRootDirectory(), transferTask.getRemoteRootDirectory()
		);
		DirectoryStructure localDirectoryStructure = directoryStructureReader.readDirectoryStructure(
			transferTask.getLocalRootDirectory(), transferTask.getInclude(), transferTask.getExclude()
		);
		UploadSessionClient uploadSessionClient = uploadClient.initializeUpload(
			transferTask.getRemoteRootDirectory(),
			transferTask.getOptions().getChunkSizeBytes(),
			transferTask.getInclude(), transferTask.getExclude(),
			localDirectoryStructure
		);
		exitTransferAborter.registerAbort(uploadSessionClient);
		DirectoryStructure remoteDirectoryStructure = uploadSessionClient.getDirectoryStructure();
		Map<FilePath, FileMetadata> remoteFilesMetadata = remoteDirectoryStructure.filesMetadataAsMap();
		int numTransferConnections = transferTask.getOptions().getNumTransferConnections();
		ExecutorService fileUploadService = Executors.newFixedThreadPool(numTransferConnections, fileThreadFactory);
		ExecutorService chunkUploadService = Executors.newFixedThreadPool(numTransferConnections, chunkThreadFactory);
		try (UploadSessionExecutor uploadSessionExecutor = new UploadSessionExecutor(
			fileUploadService, chunkUploadService, transferTask,
			localDirectoryStructure, remoteDirectoryStructure,
			remoteFilesMetadata, uploadSessionClient, progressListener
		)) {
			uploadSessionExecutor.doUploadSession();
		} catch (Exception ex) {
			progressListener.aborted();
			runIgnoreException(uploadSessionClient::abortUpload);
			throw ex;
		}
	}

	@AllArgsConstructor
	private class UploadSessionExecutor implements AutoCloseable {

		private final ExecutorService fileUploadService;
		private final ExecutorService chunkUploadService;
		private final TransferTask transferTask;
		private final DirectoryStructure localDirectoryStructure;
		private final DirectoryStructure remoteDirectoryStructure;
		private final Map<FilePath, FileMetadata> remoteFilesMetadata;
		private final UploadSessionClient uploadSessionClient;
		private final TransferProgressListener progressListener;

		void doUploadSession() throws IOException {
			TransferOptions options = transferTask.getOptions();
			progressListener.started(localDirectoryStructure, options.getChunkSizeBytes());
			uploadSessionClient.createDirectories();
			if (options.isDeleteUnexpected()) {
				unexpectedFilesDeleter.deleteUnexpected(
					progressListener,
					localDirectoryStructure, remoteDirectoryStructure,
					uploadSessionClient::deleteFiles
				);
			}
			SizeClassifiedFiles sizeClassifiedFiles = fileSizeClassifier.classifySmallAndBigFiles(
				localDirectoryStructure.getFiles(), options.getChunkSizeBytes(), options.getNumTransferConnections()
			);
			List<CompletableFuture<Void>> smallFilesFutures = StreamEx.of(sizeClassifiedFiles.getSmallFiles())
				.map(smallLocalFileMetadata -> CompletableFuture.runAsync(
					() -> syncFile(smallLocalFileMetadata), fileUploadService
				))
				.toList();
			smallFilesFutures.forEach(CompletableFuture::join);
			sizeClassifiedFiles.getBigFiles().forEach(this::syncFile);
			uploadSessionClient.commitUpload();
			progressListener.completed();
		}

		private void syncFile(FileMetadata localFileMetadata) {
			FileMetadata remoteFileMetadata = remoteFilesMetadata.get(localFileMetadata.getFilePath());
			FileStatus remoteFileStatus = Optional.ofNullable(remoteFileMetadata)
				.map(FileStatus::of)
				.orElse(FileStatus.NOT_EXIST);
			progressListener.fileAnalyze(localFileMetadata.getFilePath());
			FileFilterResult fileFilterResult = fileTransferFilter.checkIsUploadTransferNeeded(
				transferTask.getLocalRootDirectory(), localFileMetadata, remoteFileStatus, uploadSessionClient, transferTask.getOptions()
			);
			FilePath filePath = localFileMetadata.getFilePath();
			switch (fileFilterResult.getCheckResultType()) {
				case SKIP_TRANSFER:
					uploadSessionClient.skipFileUpload(filePath);
					progressListener.fileSkipped(filePath);
					break;
				case TRANSFER_FULLY:
					doSyncFile(
						localFileMetadata,
						Collections.emptyMap(),
						Collections.emptyMap()
					);
					break;
				case TRANSFER_PARTIALLY:
					doSyncFile(
						localFileMetadata,
						fileFilterResult.sourceChunkDigestsOrEmptyMap(),
						fileFilterResult.destinationChunkDigestsOrEmptyMap()
					);
					break;
				case ONLY_SET_METADATA:
					setRemoteFileMetadata(localFileMetadata);
					progressListener.fileSkipped(filePath);
					break;
				default:
					throw new IllegalArgumentException("Unknown CheckResultType: " + fileFilterResult.getCheckResultType());
			}
		}

		private void doSyncFile(
			FileMetadata fileMetadata,
			Map<ChunkInfo, byte[]> localFileChunkDigests,
			Map<ChunkInfo, byte[]> remoteFileChunkDigests
		) {
			FilePath filePath = fileMetadata.getFilePath();
			FileUploadSessionClient fileUploadSessionClient = uploadSessionClient.initializeFileUpload(filePath);
			progressListener.fileStarted(filePath);
			String localRootDirectory = transferTask.getLocalRootDirectory();
			File file = new File(filePath.toAbsolutePath(localRootDirectory));
			try (RandomAccessFilePool randomAccessFilePool = new RandomAccessFilePool(file, Mode.READ_ONLY)) {
				List<ChunkInfo> chunkInfos = chunkInfosGenerator.generateChunkInfos(
					fileMetadata.getFileSizeBytes(), transferTask.getOptions().getChunkSizeBytes()
				);
				List<CompletableFuture<Void>> chunkCompletables = new ArrayList<>();
				for (ChunkInfo chunkInfo : chunkInfos) {
					byte[] localChunkDigest = localFileChunkDigests.get(chunkInfo);
					byte[] remoteChunkDigest = remoteFileChunkDigests.get(chunkInfo);
					if (localChunkDigest != null && remoteChunkDigest != null && Arrays.equals(localChunkDigest, remoteChunkDigest)) {
						//skipping transfer of whole chunk
						progressListener.fileChunkSkipped(filePath, chunkInfo);
						continue;
					}
					chunkCompletables.add(CompletableFuture.runAsync(
						() -> uploadChunk(filePath, fileUploadSessionClient, randomAccessFilePool, chunkInfo),
						chunkUploadService
					));
				}
				chunkCompletables.forEach(CompletableFuture::join);
			}
			fileUploadSessionClient.commitFileUpload();
			progressListener.fileCompleted(filePath);
		}

		private void uploadChunk(
			FilePath filePath,
			FileUploadSessionClient fileUploadSessionClient,
			RandomAccessFilePool randomAccessFilePool,
			ChunkInfo chunkInfo
		) {
			//try-with-resources so that randomAccessFile gets returned into pool
			try (ReturnableRandomAccessFile randomAccessFile = randomAccessFilePool.borrowFile()) {
				InputStream chunkInputStream = new FileChunkInputStream(
					randomAccessFile, chunkInfo.getOffsetBytes(), chunkInfo.getSizeBytes()
				);
				InputStream observableChunkInputStream = new ProgressObservableInputStream(
					chunkInputStream, numBytes -> progressListener.fileChunkTransferProgress(filePath, chunkInfo, numBytes)
				);
				fileUploadSessionClient.uploadChunk(chunkInfo, observableChunkInputStream);
			} catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
			progressListener.fileChunkTransferred(filePath, chunkInfo);
		}

		private void setRemoteFileMetadata(FileMetadata fileMetadata) {
			FileUploadSessionClient fileUploadSessionClient = uploadSessionClient.initializeFileUpload(fileMetadata.getFilePath());
			fileUploadSessionClient.commitFileUpload();
		}

		@Override
		public void close() {
			fileUploadService.shutdown();
			chunkUploadService.shutdown();
		}
	}

}
