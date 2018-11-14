package com.mediatoolkit.pareco.components;

import java.io.IOException;
import java.io.InputStream;
import lombok.AllArgsConstructor;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 30/10/2018
 */
@AllArgsConstructor
public class ProgressObservableInputStream extends InputStream {

	private final InputStream inputStream;
	private final InputStreamProgressObserver progressObserver;

	@Override
	public int read() throws IOException {
		return inputStream.read();
	}

	@Override
	public int read(byte[] buf) throws IOException {
		int read = inputStream.read(buf);
		if (read > 0) {
			progressObserver.progress(read);
		}
		return read;
	}

	@Override
	public int read(byte[] buf, int off, int len) throws IOException {
		int read = inputStream.read(buf, off, len);
		if (read > 0) {
			progressObserver.progress(read);
		}
		return read;
	}

	@Override
	public long skip(long n) throws IOException {
		long skip = inputStream.skip(n);
		progressObserver.progress(skip);
		return skip;
	}

	@Override
	public int available() throws IOException {
		return inputStream.available();
	}

	@Override
	public void close() throws IOException {
		inputStream.close();
	}

	@Override
	public synchronized void mark(int readLimit) {
		inputStream.mark(readLimit);
	}

	@Override
	public synchronized void reset() throws IOException {
		inputStream.reset();
	}

	@Override
	public boolean markSupported() {
		return inputStream.markSupported();
	}

	public interface InputStreamProgressObserver {

		void progress(long numBytes);

	}
}
