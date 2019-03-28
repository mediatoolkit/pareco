package com.mediatoolkit.pareco.transfer.download;

import com.mediatoolkit.pareco.components.ChunkInfosGenerator;
import com.mediatoolkit.pareco.components.DirectoryStructureReader;
import com.mediatoolkit.pareco.components.DirectoryWriter;
import com.mediatoolkit.pareco.components.FileChunkWriter;
import com.mediatoolkit.pareco.components.FileDeleter;
import com.mediatoolkit.pareco.components.MetadataWriter;
import com.mediatoolkit.pareco.components.ProgressObservableInputStream;
import com.mediatoolkit.pareco.components.RandomAccessFilePool;
import com.mediatoolkit.pareco.components.RandomAccessFilePool.Mode;
import com.mediatoolkit.pareco.components.RandomAccessFilePool.ReturnableRandomAccessFile;
import com.mediatoolkit.pareco.components.TransferNamesEncoding;
import com.mediatoolkit.pareco.exceptions.UnknownTransferException;
import com.mediatoolkit.pareco.model.ChunkInfo;
import com.mediatoolkit.pareco.model.DirectoryStructure;
import com.mediatoolkit.pareco.model.FileMetadata;
import com.mediatoolkit.pareco.model.FilePath;
import com.mediatoolkit.pareco.model.FileStatus;
import com.mediatoolkit.pareco.progress.TransferProgressListener;
import com.mediatoolkit.pareco.restclient.DownloadClient;
import com.mediatoolkit.pareco.restclient.DownloadClient.DownloadSessionClient;
import com.mediatoolkit.pareco.restclient.DownloadClient.FileDownloadSessionClient;
import com.mediatoolkit.pareco.restclient.TransferClientException.ServerSideTransferClientException.FileDeletedOnServerSideException;
import com.mediatoolkit.pareco.transfer.FileSizeClassifier;
import com.mediatoolkit.pareco.transfer.FileTransferFilter;
import com.mediatoolkit.pareco.transfer.UnexpectedFilesDeleter;
import com.mediatoolkit.pareco.transfer.exit.TransferAbortTrigger;
import com.mediatoolkit.pareco.transfer.model.FileFilterResult;
import com.mediatoolkit.pareco.transfer.model.ServerInfo;
import com.mediatoolkit.pareco.transfer.model.SizeClassifiedFiles;
import com.mediatoolkit.pareco.transfer.model.TransferOptions;
import com.mediatoolkit.pareco.transfer.model.TransferTask;
import static com.mediatoolkit.pareco.util.Util.runIgnoreException;
import static com.mediatoolkit.pareco.util.Util.uncheckedConsumerSneaky;
import static com.mediatoolkit.pareco.util.Util.uncheckedRunnableSneaky;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import static java.util.Collections.singletonList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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
public class DownloadTransferExecutor {

	private final ThreadFactory chunkThreadFactory = new CustomizableThreadFactory("ChunkDownload_");
	private final ThreadFactory fileThreadFactory = new CustomizableThreadFactory("FileDownload_");

	private final DirectoryStructureReader directoryStructureReader;
	private final UnexpectedFilesDeleter unexpectedFilesDeleter;
	private final FileSizeClassifier fileSizeClassifier;
	private final FileDeleter fileDeleter;
	private final FileTransferFilter fileTransferFilter;
	private final DirectoryWriter directoryWriter;
	private final FileChunkWriter fileChunkWriter;
	private final ChunkInfosGenerator chunkInfosGenerator;
	private final MetadataWriter metadataWriter;
	private final TransferNamesEncoding encoding;

	public void executeDownload(
		TransferTask transferTask,
		TransferAbortTrigger abortTrigger,
		TransferProgressListener progressListener
	) throws IOException {
		ServerInfo serverInfo = transferTask.getServerInfo();
		DownloadClient downloadClient = DownloadClient.builder()
			.httpScheme(serverInfo.getHttpScheme())
			.host(serverInfo.getHost())
			.port(serverInfo.getPort())
			.connectTimeout(transferTask.getOptions().getConnectTimeout())
			.readTimeout(transferTask.getOptions().getTimeout())
			.authToken(transferTask.getAuthToken())
			.encoding(encoding)
			.build();
		progressListener.initializing(
			"download",
			transferTask.getRemoteRootDirectory(), transferTask.getLocalRootDirectory(),
			serverInfo.toUrl()
		);
		DownloadSessionClient downloadSessionClient = downloadClient.initializeDownload(
			transferTask.getRemoteRootDirectory(), transferTask.getOptions().getChunkSizeBytes(),
			transferTask.getInclude(), transferTask.getExclude()
		);
		try {
			abortTrigger.registerAbort(downloadSessionClient::abortDownload);
			progressListener.analyzingFiles(transferTask.getRemoteRootDirectory(), transferTask.getLocalRootDirectory());
			DirectoryStructure remoteDirectoryStructure = downloadSessionClient.getDirectoryStructure();
			DirectoryStructure localDirectoryStructure = directoryStructureReader.readDirectoryStructure(
				transferTask.getLocalRootDirectory(), transferTask.getInclude(), transferTask.getExclude()
			);
			Map<FilePath, FileMetadata> localFilesMetadata = localDirectoryStructure.filesMetadataAsMap();
			int numTransferConnections = transferTask.getOptions().getNumTransferConnections();
			ExecutorService fileDownloadService = Executors.newFixedThreadPool(numTransferConnections, fileThreadFactory);
			ExecutorService chunkDownloadService = Executors.newFixedThreadPool(numTransferConnections, chunkThreadFactory);
			try (DownloadSessionExecutor downloadSessionExecutor = new DownloadSessionExecutor(
				fileDownloadService, chunkDownloadService, transferTask,
				remoteDirectoryStructure, localDirectoryStructure,
				localFilesMetadata, downloadSessionClient, progressListener
			)) {
				downloadSessionExecutor.doDownloadSession();
			}
		} catch (Exception ex) {
			progressListener.aborted();
			runIgnoreException(downloadSessionClient::abortDownload);
			throw ex;
		}
	}

	@AllArgsConstructor
	private class DownloadSessionExecutor implements AutoCloseable {

		private final ExecutorService fileDownloadService;
		private final ExecutorService chunkDownloadService;
		private final TransferTask transferTask;
		private final DirectoryStructure remoteDirectoryStructure;
		private final DirectoryStructure localDirectoryStructure;
		private final Map<FilePath, FileMetadata> localFilesMetadata;
		private final DownloadSessionClient downloadSessionClient;
		private final TransferProgressListener progressListener;

		private void doDownloadSession() throws IOException {
			TransferOptions options = transferTask.getOptions();
			progressListener.started(remoteDirectoryStructure, options.getChunkSizeBytes());
			directoryWriter.createDirectories(
				transferTask.getLocalRootDirectory(), remoteDirectoryStructure.getDirectories()
			);
			if (options.isDeleteUnexpected()) {
				unexpectedFilesDeleter.deleteUnexpected(
					progressListener,
					remoteDirectoryStructure, localDirectoryStructure,
					filePaths -> fileDeleter.delete(
						transferTask.getLocalRootDirectory(), filePaths
					)
				);
			}
			SizeClassifiedFiles sizeClassifiedFiles = fileSizeClassifier.classifySmallAndBigFiles(
				remoteDirectoryStructure.getFiles(), options.getChunkSizeBytes(), options.getNumTransferConnections()
			);
			List<CompletableFuture<Void>> smallFilesFutures = StreamEx.of(sizeClassifiedFiles.getSmallFiles())
				.map(smallRemoteFileMetadata -> CompletableFuture.runAsync(
					uncheckedRunnableSneaky(() -> syncFile(smallRemoteFileMetadata)), fileDownloadService
				))
				.toList();
			smallFilesFutures.forEach(CompletableFuture::join);
			sizeClassifiedFiles.getBigFiles().forEach(uncheckedConsumerSneaky(this::syncFile));
			downloadSessionClient.commitDownload();
			progressListener.completed();
		}

		private void syncFile(FileMetadata remoteFileMetadata) throws IOException {
			FileMetadata localFileMetadata = localFilesMetadata.get(remoteFileMetadata.getFilePath());
			FileStatus localFileStatus = Optional.ofNullable(localFileMetadata)
				.map(FileStatus::of)
				.orElse(FileStatus.NOT_EXIST);
			String localRootDirectory = transferTask.getLocalRootDirectory();
			progressListener.fileAnalyze(remoteFileMetadata.getFilePath());
			FileFilterResult fileFilterResult = fileTransferFilter.checkIsDownloadTransferNeeded(
				localRootDirectory, localFileStatus, remoteFileMetadata, downloadSessionClient, transferTask.getOptions()
			);
			FilePath filePath = remoteFileMetadata.getFilePath();
			switch (fileFilterResult.getCheckResultType()) {
				case SKIP_TRANSFER:
					downloadSessionClient.skipFileDownload(filePath);
					progressListener.fileSkipped(filePath);
					break;
				case TRANSFER_FULLY:
					doSyncFile(
						remoteFileMetadata,
						Collections.emptyMap(),
						Collections.emptyMap()
					);
					break;
				case TRANSFER_PARTIALLY:
					doSyncFile(
						remoteFileMetadata,
						fileFilterResult.sourceChunkDigestsOrEmptyMap(),
						fileFilterResult.destinationChunkDigestsOrEmptyMap()
					);
					break;
				case ONLY_SET_METADATA:
					metadataWriter.writeFileMetadata(localRootDirectory, remoteFileMetadata);
					progressListener.fileSkipped(filePath);
					break;
				default:
					throw new IllegalArgumentException("Unknown CheckResultType: " + fileFilterResult.getCheckResultType());
			}
		}

		private void doSyncFile(
			FileMetadata remoteFileMetadata,
			Map<ChunkInfo, byte[]> localFileChunkDigests,
			Map<ChunkInfo, byte[]> remoteFileChunkDigests
		) throws IOException {
			FilePath filePath = remoteFileMetadata.getFilePath();
			FileDownloadSessionClient fileDownloadSessionClient = downloadSessionClient.initializeFileDownload(filePath);
			progressListener.fileStarted(filePath);
			String localRootDirectory = transferTask.getLocalRootDirectory();
			File file = new File(filePath.toAbsolutePath(localRootDirectory));
			try (RandomAccessFilePool randomAccessFilePool = new RandomAccessFilePool(file, Mode.READ_WRITE)) {
				try (ReturnableRandomAccessFile randomAccessFile = randomAccessFilePool.borrowFile()) {
					fileChunkWriter.allocateFileToSize(randomAccessFile, remoteFileMetadata.getFileSizeBytes());
				}
				List<ChunkInfo> chunkInfos = chunkInfosGenerator.generateChunkInfos(
					remoteFileMetadata.getFileSizeBytes(), transferTask.getOptions().getChunkSizeBytes()
				);
				List<CompletableFuture<Void>> chunkCompletables = new ArrayList<>();
				for (ChunkInfo chunkInfo : chunkInfos) {
					boolean skipChunk = fileTransferFilter.skipChunkIfNeeded(
						localFileChunkDigests, remoteFileChunkDigests, filePath, chunkInfo, progressListener
					);
					if (skipChunk) {
						continue;
					}
					chunkCompletables.add(CompletableFuture.runAsync(
						() -> downloadChunk(filePath, fileDownloadSessionClient, randomAccessFilePool, chunkInfo),
						chunkDownloadService
					));
				}
				try {
					chunkCompletables.forEach(CompletableFuture::join);
					metadataWriter.writeFileMetadata(localRootDirectory, remoteFileMetadata);
				} catch (CompletionException completionEx) {
					Throwable cause = completionEx.getCause();
					if (cause instanceof FileDeletedOnServerSideException) {
						fileDeleter.delete(transferTask.getLocalRootDirectory(), singletonList(filePath));
						progressListener.fileDeleted(filePath);
					} else {
						throw new UnknownTransferException(completionEx);
					}
				} finally {
					chunkCompletables.forEach(future -> future.cancel(false));
				}
			}
			fileDownloadSessionClient.commitFileDownload();
			progressListener.fileCompleted(filePath);
		}

		private void downloadChunk(
			FilePath filePath,
			FileDownloadSessionClient fileDownloadSessionClient,
			RandomAccessFilePool randomAccessFilePool,
			ChunkInfo chunkInfo
		) {
			//try-with-resources so that randomAccessFile gets returned into pool
			try (ReturnableRandomAccessFile randomAccessFile = randomAccessFilePool.borrowFile()) {
				fileDownloadSessionClient.downloadChunk(chunkInfo, inputStream -> fileChunkWriter.writeChunk(
					randomAccessFile,
					chunkInfo,
					new ProgressObservableInputStream(
						inputStream,
						numBytes -> progressListener.fileChunkTransferProgress(filePath, chunkInfo, numBytes)
					)
				));
			} catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
			progressListener.fileChunkTransferred(filePath, chunkInfo);
		}

		@Override
		public void close() {
			fileDownloadService.shutdown();
			chunkDownloadService.shutdown();
		}
	}

}
