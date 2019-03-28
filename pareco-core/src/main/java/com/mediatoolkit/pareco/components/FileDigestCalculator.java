package com.mediatoolkit.pareco.components;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashingInputStream;
import com.mediatoolkit.pareco.exceptions.FileDeletedException;
import com.mediatoolkit.pareco.model.ChunkInfo;
import com.mediatoolkit.pareco.model.DigestType;
import com.mediatoolkit.pareco.model.FileDigest;
import com.mediatoolkit.pareco.model.FileDigest.ChunkDigest;
import com.mediatoolkit.pareco.model.FilePath;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 24/10/2018
 */
@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class FileDigestCalculator {

	private static final int BUF_SIZE = 64 * 1024;

	private final ChunkInfosGenerator chunkInfosGenerator;

	public FileDigest calculateFileDigest(
		String rootDirectory, FilePath filePath, long chunkSizeBytes, DigestType digestType
	) throws IOException {
		File file = new File(filePath.toAbsolutePath(rootDirectory));
		long totalSizeBytes = getFileSize(file);
		List<ChunkInfo> chunkInfos = chunkInfosGenerator.generateChunkInfos(totalSizeBytes, chunkSizeBytes);
		List<ChunkDigest> chunkDigests = new ArrayList<>(chunkInfos.size());
		try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
			for (ChunkInfo chunkInfo : chunkInfos) {
				byte[] digest = calculateChunkDigest(randomAccessFile, chunkInfo, digestType);
				chunkDigests.add(ChunkDigest.of(digest, chunkInfo));
			}
		} catch (FileNotFoundException ex) {
			throw new FileDeletedException(filePath, "Can't calc digest of deleted file", ex);
		}
		return FileDigest.builder()
			.digestType(digestType)
			.chunkDigests(chunkDigests)
			.build();
	}

	private long getFileSize(File file) throws FileNotFoundException {
		long totalSize = file.length();
		if (totalSize == 0 && !file.exists()) {
			throw new FileNotFoundException(file.getPath());
		}
		return totalSize;
	}

	private byte[] calculateChunkDigest(
		RandomAccessFile file, ChunkInfo chunkInfo, DigestType digestType
	) throws IOException {
		InputStream inputStream = new FileChunkInputStream(
			file, chunkInfo.getOffsetBytes(), chunkInfo.getSizeBytes()
		);
		HashingInputStream hashingInputStream = new HashingInputStream(
			digestType.getHashFunction(), inputStream
		);
		IOUtils.copy(hashingInputStream, NullOutputStream.NULL_OUTPUT_STREAM, BUF_SIZE);
		HashCode hash = hashingInputStream.hash();
		return hash.asBytes();
	}


}
