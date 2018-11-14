package com.mediatoolkit.pareco.transfer;

import com.mediatoolkit.pareco.components.FileDigestCalculator;
import com.mediatoolkit.pareco.transfer.model.FileFilterResult;
import com.mediatoolkit.pareco.transfer.model.TransferOptions;
import static com.mediatoolkit.pareco.util.Util.uncheckedSupplierSneaky;
import com.mediatoolkit.pareco.model.DigestType;
import com.mediatoolkit.pareco.model.FileDigest;
import com.mediatoolkit.pareco.model.FileMetadata;
import com.mediatoolkit.pareco.model.FilePath;
import com.mediatoolkit.pareco.model.FileStatus;
import com.mediatoolkit.pareco.restclient.DownloadClient.DownloadSessionClient;
import com.mediatoolkit.pareco.restclient.UploadClient.UploadSessionClient;
import com.mediatoolkit.pareco.transfer.model.FileFilterResult.CheckResultType;
import com.mediatoolkit.pareco.transfer.model.TransferOptions.FileIntegrityOptions;
import com.mediatoolkit.pareco.transfer.model.TransferOptions.IntegrityCheckType;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
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
				if (!fileMetadataOk) {
					return FileFilterResult.builder()
						.checkResultType(CheckResultType.TRANSFER_PARTIALLY)
						.build();
				}
				return resultThatFileIsOk(neededToSetPermissions);
			case FILE_METADATA_AND_DIGEST:
				CompletableFuture<FileDigest> sourceFileDigestFuture = CompletableFuture.supplyAsync(
					sourceFileDigestSupplier, digestCalcService
				);
				CompletableFuture<FileDigest> destinationFileDigestFuture = CompletableFuture.supplyAsync(
					destinationFileDigestSupplier, digestCalcService
				);
				FileDigest sourceFileDigest = sourceFileDigestFuture.join();
				FileDigest destinationFileDigest = destinationFileDigestFuture.join();
				if (fileMetadataOk && destinationFileDigest.equals(sourceFileDigest)) {
					return resultThatFileIsOk(neededToSetPermissions);
				} else {
					return FileFilterResult.builder()
						.checkResultType(CheckResultType.TRANSFER_PARTIALLY)
						.sourceFileDigest(sourceFileDigest)
						.destinationFileDigest(destinationFileDigest)
						.build();
				}
			default:
				throw new IllegalArgumentException("Unknown integrity check type: " + integrityCheckType);
		}
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


}
