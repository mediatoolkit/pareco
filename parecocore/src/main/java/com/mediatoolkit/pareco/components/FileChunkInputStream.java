package com.mediatoolkit.pareco.components;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import static org.apache.commons.io.IOUtils.EOF;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 24/10/2018
 */
public class FileChunkInputStream extends InputStream {

	private final RandomAccessFile file;
	private long pos;
	private long max;

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

	@Override
	public void close() throws IOException {
		file.close();
	}

}


