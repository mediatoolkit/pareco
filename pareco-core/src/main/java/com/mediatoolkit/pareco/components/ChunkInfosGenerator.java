package com.mediatoolkit.pareco.components;

import com.mediatoolkit.pareco.model.ChunkInfo;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 28/10/2018
 */
@Component
public class ChunkInfosGenerator {

	public List<ChunkInfo> generateChunkInfos(long totalSizeBytes, long chunkSizeBytes) {
		int numChunks = (int) ((totalSizeBytes + chunkSizeBytes - 1) / chunkSizeBytes);
		List<ChunkInfo> chunkInfos = new ArrayList<>(numChunks);
		for (long offset = 0; offset < totalSizeBytes; offset += chunkSizeBytes) {
			long endOffset = Math.min(offset + chunkSizeBytes, totalSizeBytes);
			long sizeBytes = endOffset - offset;
			chunkInfos.add(ChunkInfo.of(offset, sizeBytes));
		}
		return chunkInfos;
	}

}
