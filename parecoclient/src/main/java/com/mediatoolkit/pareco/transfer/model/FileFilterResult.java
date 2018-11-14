package com.mediatoolkit.pareco.transfer.model;

import com.mediatoolkit.pareco.model.ChunkInfo;
import com.mediatoolkit.pareco.model.FileDigest;
import java.util.Collections;
import java.util.Map;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 29/10/2018
 */
@Value
@Builder
public class FileFilterResult {

	public enum CheckResultType {
		SKIP_TRANSFER,
		ONLY_SET_METADATA,
		TRANSFER_FULLY,
		TRANSFER_PARTIALLY
	}

	@NonNull
	private CheckResultType checkResultType;
	private FileDigest sourceFileDigest;
	private FileDigest destinationFileDigest;

	public Map<ChunkInfo, byte[]> sourceChunkDigestsOrEmptyMap() {
		if (sourceFileDigest == null) {
			return Collections.emptyMap();
		} else {
			return sourceFileDigest.toChunkInfoMap();
		}
	}

	public Map<ChunkInfo, byte[]> destinationChunkDigestsOrEmptyMap() {
		if (destinationFileDigest == null) {
			return Collections.emptyMap();
		} else {
			return destinationFileDigest.toChunkInfoMap();
		}
	}
}
