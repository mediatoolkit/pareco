package com.mediatoolkit.pareco.transfer;

import com.mediatoolkit.pareco.components.FileDigestCalculator;
import com.mediatoolkit.pareco.model.ChunkInfo;
import com.mediatoolkit.pareco.model.DigestType;
import com.mediatoolkit.pareco.model.FileDigest;
import com.mediatoolkit.pareco.model.FileMetadata;
import com.mediatoolkit.pareco.model.FilePath;
import com.mediatoolkit.pareco.model.FileStatus;
import com.mediatoolkit.pareco.progress.TransferProgressListener;
import com.mediatoolkit.pareco.restclient.DownloadClient.DownloadSessionClient;
import com.mediatoolkit.pareco.restclient.UploadClient.UploadSessionClient;
import com.mediatoolkit.pareco.transfer.model.FileFilterResult;
import com.mediatoolkit.pareco.transfer.model.FileFilterResult.CheckResultType;
import com.mediatoolkit.pareco.transfer.model.TransferOptions;
import com.mediatoolkit.pareco.transfer.model.TransferOptions.FileIntegrityOptions;
import com.mediatoolkit.pareco.transfer.model.TransferOptions.IntegrityCheckType;
import static com.mediatoolkit.pareco.util.Util.uncheckedSupplierSneaky;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.Value;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 29/10/2018
 */
@Component
public class FileTransferFilter implements AutoCloseable {

	private final FileDigestCalculator fileDigestCalculator;
	private final ExecutorService digestCalcService;

	public FileTransferFilter(FileDigestCalculator fileDigestCalculator) {
		this.fileDigestCalculator = fileDigestCalculator;
		this.digestCalcService = Executors.newCachedThreadPool(
			new CustomizableThreadFactory("digestCalculator")
		);
	}

	public FileFilterResult checkIsDownloadTransferNeeded(
		String localRootDirectory, FileStatus localFileStatus, FileMetadata remoteFileMetadata,
		DownloadSessionClient downloadSessionClient, TransferOptions options
	) {
		FilePath filePath = remoteFileMetadata.getFilePath();
		DigestType digestType = options.getFileIntegrityOptions().getDigestType();
		return checkIsTransferNeeded(
			options,
			remoteFileMetadata,
			localFileStatus,
			() -> downloadSessionClient.getFileDigest(filePath, digestType),
			uncheckedSupplierSneaky(() -> fileDigestCalculator.calculateFileDigest(
				localRootDirectory, filePath, options.getChunkSizeBytes(), digestType
			))
		);
	}

	public FileFilterResult checkIsUploadTransferNeeded(
		String localRootDirectory, FileMetadata localFileMetadata, FileStatus remoteFileStatus,
		UploadSessionClient uploadSessionClient, TransferOptions options
	) {
		FilePath filePath = localFileMetadata.getFilePath();
		DigestType digestType = options.getFileIntegrityOptions().getDigestType();
		return checkIsTransferNeeded(
			options,
			localFileMetadata,
			remoteFileStatus,
			uncheckedSupplierSneaky(() -> fileDigestCalculator.calculateFileDigest(
				localRootDirectory, filePath, options.getChunkSizeBytes(), digestType
			)),
			() -> uploadSessionClient.getFileDigest(filePath, digestType)
		);
	}

	public boolean skipChunkIfNeeded(
		Map<ChunkInfo, byte[]> localFileChunkDigests,
		Map<ChunkInfo, byte[]> remoteFileChunkDigests,
		FilePath filePath,
		ChunkInfo chunkInfo,
		TransferProgressListener progressListener
	) {
		byte[] localChunkDigest = localFileChunkDigests.get(chunkInfo);
		byte[] remoteChunkDigest = remoteFileChunkDigests.get(chunkInfo);
		if (localChunkDigest != null && remoteChunkDigest != null && Arrays.equals(localChunkDigest, remoteChunkDigest)) {
			//skipping transfer of whole chunk
			progressListener.fileChunkSkipped(filePath, chunkInfo);
			return true;
		}
		return false;
	}

	private FileFilterResult checkIsTransferNeeded(
		TransferOptions options,
		FileMetadata sourceFileMetadata,
		FileStatus destinationFileStatus,
		Supplier<FileDigest> sourceFileDigestSupplier,
		Supplier<FileDigest> destinationFileDigestSupplier
	) {
		FileIntegrityOptions fileIntegrityOptions = options.getFileIntegrityOptions();
		IntegrityCheckType integrityCheckType = fileIntegrityOptions.getIntegrityCheckType();
		if (!destinationFileStatus.isExist()) {
			return FileFilterResult.builder()
				.checkResultType(CheckResultType.TRANSFER_FULLY)
				.build();
		}
		FileMetadata destinationFileMetadata = destinationFileStatus.getFileMetadata();
		boolean fileMetadataOk = isFileMetadataOk(sourceFileMetadata, destinationFileMetadata);
		boolean neededToSetPermissions = isNeededToSetPermissions(sourceFileMetadata, destinationFileMetadata);
		switch (integrityCheckType) {
			case ONLY_FILE_METADATA:
				if (fileMetadataOk) {
					return resultThatFileIsOk(neededToSetPermissions);
				}
				//intentional fallthrough
			case FILE_METADATA_AND_DIGEST:
				return checkMetadataAndDigests(
					sourceFileDigestSupplier,
					destinationFileDigestSupplier,
					fileMetadataOk,
					neededToSetPermissions
				);
			default:
				throw new IllegalArgumentException("Unknown integrity check type: " + integrityCheckType);
		}
	}

	private FileFilterResult checkMetadataAndDigests(Supplier<FileDigest> sourceFileDigestSupplier, Supplier<FileDigest> destinationFileDigestSupplier, boolean fileMetadataOk, boolean neededToSetPermissions) {
		SrcDstFileDigests fileDigests = calculateFileDigests(
			sourceFileDigestSupplier, destinationFileDigestSupplier
		);
		if (fileMetadataOk && fileDigests.sourceEqualToDestination()) {
			return resultThatFileIsOk(neededToSetPermissions);
		} else {
			return FileFilterResult.builder()
				.checkResultType(CheckResultType.TRANSFER_PARTIALLY)
				.sourceFileDigest(fileDigests.srcFileDigest)
				.destinationFileDigest(fileDigests.dstFileDigest)
				.build();
		}
	}

	private SrcDstFileDigests calculateFileDigests(Supplier<FileDigest> sourceFileDigestSupplier, Supplier<FileDigest> destinationFileDigestSupplier) {
		CompletableFuture<FileDigest> sourceFileDigestFuture = CompletableFuture.supplyAsync(
			sourceFileDigestSupplier, digestCalcService
		);
		CompletableFuture<FileDigest> destinationFileDigestFuture = CompletableFuture.supplyAsync(
			destinationFileDigestSupplier, digestCalcService
		);
		FileDigest sourceFileDigest = sourceFileDigestFuture.join();
		FileDigest destinationFileDigest = destinationFileDigestFuture.join();
		return SrcDstFileDigests.of(sourceFileDigest, destinationFileDigest);
	}

	private FileFilterResult resultThatFileIsOk(boolean neededToSetPermissions) {
		if (neededToSetPermissions) {
			return FileFilterResult.builder()
				.checkResultType(CheckResultType.ONLY_SET_METADATA)
				.build();
		} else {
			return FileFilterResult.builder()
				.checkResultType(CheckResultType.SKIP_TRANSFER)
				.build();
		}
	}

	private boolean isFileMetadataOk(
		FileMetadata sourceFileMetadata, FileMetadata destinationFileMetadata
	) {
		long sourceFileSize = sourceFileMetadata.getFileSizeBytes();
		long destinationFileSize = destinationFileMetadata.getFileSizeBytes();
		long sourceLastModified = sourceFileMetadata.getLastModifiedTimeMillis();
		long destinationLastModified = destinationFileMetadata.getLastModifiedTimeMillis();
		boolean fileSizeOk = sourceFileSize == destinationFileSize;
		boolean lastModifiedOk = sourceLastModified <= destinationLastModified;
		return fileSizeOk && lastModifiedOk;
	}

	private boolean isNeededToSetPermissions(
		FileMetadata sourceFileMetadata, FileMetadata destinationFileMetadata
	) {
		Set<PosixFilePermission> sourcePermissions = sourceFileMetadata.getPermissions();
		Set<PosixFilePermission> destinationPermissions = destinationFileMetadata.getPermissions();
		boolean sourceAndDestinationSupportPosix = sourcePermissions != null && destinationPermissions != null;
		return sourceAndDestinationSupportPosix && !sourcePermissions.equals(destinationPermissions);
	}

	@Override
	public void close() {
		digestCalcService.shutdown();
	}

	@Value(staticConstructor = "of")
	private static class SrcDstFileDigests {

		@NonNull
		private FileDigest srcFileDigest;
		@NonNull
		private FileDigest dstFileDigest;

		boolean sourceEqualToDestination() {
			return srcFileDigest.equals(dstFileDigest);
		}
	}

}
