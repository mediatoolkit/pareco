package com.mediatoolkit.pareco.model;

import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Singular;
import one.util.streamex.StreamEx;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 24/10/2018
 */
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
@Builder
public class FileDigest {

	private DigestType digestType;
	@Singular
	private List<ChunkDigest> chunkDigests;

	public Map<ChunkInfo, byte[]> toChunkInfoMap() {
		return StreamEx.of(chunkDigests)
			.mapToEntry(ChunkDigest::getChunkInfo, ChunkDigest::getDigest)
			.toMap();
	}

	@Data
	@AllArgsConstructor(staticName = "of")
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	@Setter(AccessLevel.PRIVATE)
	@Builder
	public static class ChunkDigest {

		private byte[] digest;
		private ChunkInfo chunkInfo;
	}
}
