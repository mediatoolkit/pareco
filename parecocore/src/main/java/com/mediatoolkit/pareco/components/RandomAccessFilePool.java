package com.mediatoolkit.pareco.components;

import static com.mediatoolkit.pareco.util.Util.runIgnoreException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * Pool of {@link ReturnableRandomAccessFile} instances where each file can
 * be borrowed from pool using try-with-resources construct to have each borrowed
 * file automatically returned to the pool.
 *
 * {@link ReturnableRandomAccessFile} is extended from {@link RandomAccessFile}
 * with ability to return itself to the pool.
 *
 * <pre>{@code
 * try (ReturnableRandomAccessFile file = pool.borrowFile()) {
 *     //do something with file which will be returned to the pool automatically
 * }
 * }
 * </pre>
 *
 * Closing this pool using {@link #close()} will close this pool and all files
 * present in the pool.
 * <br>
 * Closing this pool using {@link #forceClose()} will close this pool, all files
 * present in the pool and all files which are currently borrowed.
 *
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 28/10/2018
 */
public class RandomAccessFilePool implements AutoCloseable {

	@AllArgsConstructor
	@Getter
	public enum Mode {

		READ_WRITE("rw"),
		READ_ONLY("r");

		private final String mode;
	}

	private final GenericObjectPool<ReturnableRandomAccessFile> pool;
	private final Set<ReturnableRandomAccessFile> borrowedFiles = Collections.synchronizedSet(new HashSet<>());

	public RandomAccessFilePool(File file, Mode mode) {
		RandomAccessFilePoolableObjectFactory factory = new RandomAccessFilePoolableObjectFactory(
			file, mode.getMode()
		);
		pool = new GenericObjectPool<>(factory);
	}

	/**
	 * Utility method that accepts file operation which will be performed
	 * on borrowed file and after operation is completed normally or exceptionally, file will
	 * be returned to the pool.
	 *
	 * @param operation to be performed on borrowed file
	 * @throws IOException during file opening or file operation
	 */
	@SneakyThrows
	public void doOnFile(OnFileOperation operation) throws IOException {
		try (ReturnableRandomAccessFile file = borrowFile()) {
			operation.doOperation(file);
		}
	}

	@SneakyThrows
	public ReturnableRandomAccessFile borrowFile() throws IOException {
		ReturnableRandomAccessFile returnableFile = pool.borrowObject();
		returnableFile.setReturnAction(file -> {
			pool.returnObject(file);
			borrowedFiles.remove(file);
		});
		borrowedFiles.add(returnableFile);
		return returnableFile;
	}

	@SneakyThrows
	public void returnFile(ReturnableRandomAccessFile file) throws IOException {
		file.setReturnAction(null);
		pool.returnObject(file);
		borrowedFiles.remove(file);
	}

	@Override
	@SneakyThrows
	public void close() {
		pool.close();
	}

	/**
	 * Closes this pool, and closes all borrowed files
	 */
	public void forceClose() {
		runIgnoreException(this::close);
		borrowedFiles.forEach(file -> runIgnoreException(file::close));
	}

	public interface OnFileOperation {

		void doOperation(RandomAccessFile file) throws IOException;

	}

	public static class ReturnableRandomAccessFile extends RandomAccessFile implements AutoCloseable {

		@Setter
		private ReturnAction returnAction = null;

		public ReturnableRandomAccessFile(File file, String mode) throws FileNotFoundException {
			super(file, mode);
		}

		@Override
		@SneakyThrows
		public void close() throws IOException {
			if (returnAction == null) {
				super.close();
			} else {
				returnAction.returnItself(this);
			}
		}

		public interface ReturnAction {

			void returnItself(ReturnableRandomAccessFile file) throws Exception;
		}
	}

	@RequiredArgsConstructor
	private static class RandomAccessFilePoolableObjectFactory
		extends BasePoolableObjectFactory<ReturnableRandomAccessFile> {

		private final File file;
		private final String mode;

		@Override
		public ReturnableRandomAccessFile makeObject() throws Exception {
			return new ReturnableRandomAccessFile(file, mode);
		}

		@Override
		public void destroyObject(ReturnableRandomAccessFile file) throws Exception {
			file.setReturnAction(null);
			file.close();
		}
	}
}
