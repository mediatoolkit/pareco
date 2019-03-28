package com.mediatoolkit.pareco.components;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import static org.apache.commons.io.IOUtils.EOF;

/**
 * This is a input stream that reads through bounded portion of file which represents
 * a chunk in file.
 *
 * {@link RandomAccessFile} is used as underlying data source for read operations.
 *
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 24/10/2018
 */
public class FileChunkInputStream extends InputStream {

	private final RandomAccessFile file;
	private long pos;
	private long max;

	/**
	 * Creation of new instance.
	 *
	 * @param file to use as data source
	 * @param offset number of bytes to seek in file before reading
	 * @param length maximum number of bytes to read by this input stream
	 * @throws IOException which can be thrown by {@link RandomAccessFile#seek(long)}
	 */
	public FileChunkInputStream(RandomAccessFile file, long offset, long length) throws IOException {
		this.file = file;
		this.file.seek(offset);
		pos = 0;
		max = length;
	}

	@Override
	public int read() throws IOException {
		if (pos >= max) {
			return EOF;
		}
		pos++;
		return file.read();
	}

	@Override
	public int read(byte[] buf) throws IOException {
		return this.read(buf, 0, buf.length);
	}

	@Override
	public int read(byte[] buf, int off, int len) throws IOException {
		if (pos >= max) {
			return EOF;
		}
		long readLimit = Math.min(len, max - pos);
		int bytesRead = file.read(buf, off, (int) readLimit);
		if (bytesRead == EOF) {
			return EOF;
		}
		pos += bytesRead;
		return bytesRead;
	}

	/**
	 * Closes underlying {@link RandomAccessFile}. This stream does not need to be closed if
	 * if you plan to reuse given instance of {@link RandomAccessFile}.
	 * @throws IOException when {@link RandomAccessFile#close()} throws it
	 */
	@Override
	public void close() throws IOException {
		file.close();
	}

}


