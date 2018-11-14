package com.mediatoolkit.pareco.components;

import com.mediatoolkit.pareco.model.ChunkInfo;
import com.mediatoolkit.pareco.exceptions.InputStreamSizeMissMatchException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 25/10/2018
 */
@Component
public class FileChunkWriter {

	public void allocateFileToSize(RandomAccessFile file, long sizeBytes) throws IOException {
		file.setLength(sizeBytes);
	}

	public void writeChunk(
		RandomAccessFile file, ChunkInfo chunkInfo, InputStream inputStream
	) throws IOException {
		BoundedInputStream boundedInputStream = new BoundedInputStream(inputStream, chunkInfo.getSizeBytes());
		file.seek(chunkInfo.getOffsetBytes());
		long copied = copy(boundedInputStream, file);
		if (copied != chunkInfo.getSizeBytes()) {
			throw new InputStreamSizeMissMatchException(String.format(
				"Chunk input stream is expected to have %d bytes, but copied %d bytes", chunkInfo.getSizeBytes(), copied
			));
		}
		if (inputStream.available() > 0 || inputStream.read() != IOUtils.EOF) {
			throw new InputStreamSizeMissMatchException(String.format(
				"Chunk input stream is expected to have %d bytes, but it has more", chunkInfo.getSizeBytes()
			));
		}
	}

	private long copy(InputStream in, RandomAccessFile out) throws IOException {
		byte[] buffer = new byte[64 * 1024];
		int count;
		int copied = 0;
		while ((count = in.read(buffer)) > 0) {
			out.write(buffer, 0, count);
			copied += count;
		}
		return copied;
	}

}
