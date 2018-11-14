package com.mediatoolkit.pareco.transfer.model;

import com.mediatoolkit.pareco.model.DigestType;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Wither;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 28/10/2018
 */
@Value
@Builder
@Wither
public class TransferOptions {

	private int numTransferConnections;
	private int timeout;
	private boolean deleteUnexpected;
	private long chunkSizeBytes;
	@NonNull
	private FileIntegrityOptions fileIntegrityOptions;

	public enum IntegrityCheckType {
		ONLY_FILE_METADATA, FILE_METADATA_AND_DIGEST
	}

	@Value
	public static class FileIntegrityOptions {

		@NonNull
		private IntegrityCheckType integrityCheckType;
		private DigestType digestType;

		public static FileIntegrityOptions onlyMetadata() {
			return new FileIntegrityOptions(IntegrityCheckType.ONLY_FILE_METADATA, null);
		}

		public static FileIntegrityOptions metadataAndDigest(DigestType digestType) {
			return new FileIntegrityOptions(IntegrityCheckType.FILE_METADATA_AND_DIGEST, digestType);
		}

	}
}
