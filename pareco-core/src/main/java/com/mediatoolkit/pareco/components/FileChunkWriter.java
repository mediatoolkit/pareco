package com.mediatoolkit.pareco.components;

import com.mediatoolkit.pareco.exceptions.InputStreamSizeMissMatchException;
import com.mediatoolkit.pareco.model.ChunkInfo;
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

	private static final int BUFFER_SIZE = 64 * 1024;

	/**
	 * Method sets given {@code file} to have size exactly {@code sizeBytes}.
	 * If the file size before was bigger than the required size, file will be reduced and
	 * first {@code sizeBytes} bytes will remain unchanged. If the file size before was smaller
	 * than the required size, file will be expanded and first {@code sizeBytes} bytes of
	 * file will remain unchanged and contents of newly allocated bytes are undefined.
	 *
	 * @param file to allocate to given size
	 * @param sizeBytes desired size of file to be set
	 * @throws IOException
	 */
	public void allocateFileToSize(RandomAccessFile file, long sizeBytes) throws IOException {
		file.setLength(sizeBytes);
	}

	/**
	 * Method which copies contents of {@code inputStream} to {@code file} starting at offset
	 * specified by {@code chunkInfo}.
	 *
	 * @param file to copy {@code inputStream} into
	 * @param chunkInfo which defines location of where in {@code file} to write contents
	 *                  from {@code inputStream}
	 * @param inputStream to be copied into {@code file}
	 * @throws IOException if thrown when reading from {@code inputStream} or if thrown
	 * when writing to {@code file}
	 * @throws InputStreamSizeMissMatchException if {@code inputStream} has more or less
	 * bytes than size specified by {@code chunkInfo}
	 */
	public void writeChunk(
		RandomAccessFile file, ChunkInfo chunkInfo, InputStream inputStream
	) throws IOException, InputStreamSizeMissMatchException{
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
		byte[] buffer = new byte[BUFFER_SIZE];
		int count;
		int copied = 0;
		while ((count = in.read(buffer)) > 0) {
			out.write(buffer, 0, count);
			copied += count;
		}
		return copied;
	}

}
