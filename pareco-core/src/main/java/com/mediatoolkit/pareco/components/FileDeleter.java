package com.mediatoolkit.pareco.components;

import com.mediatoolkit.pareco.model.FilePath;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 31/10/2018
 */
@Component
public class FileDeleter {

	/**
	 * Method deletes all given file paths which are path representations relative to the
	 * {@code rootDirectory}. If some {@link FilePath} represents a directory, all
	 * of ith contents will be deleted recursively.
	 *
	 * This operation is not atomic and it may fail in unexpected ways if something
	 * else is performing write and delete operations in same root directory.
	 *
	 * @param rootDirectory as reference for given file paths
	 * @param filePaths     a list of relative file paths to be deleted
	 * @throws IOException if deletion of some file/directory fails, probably due to
	 * concurrent modification or invalid permissions
	 */
	public void delete(String rootDirectory, List<FilePath> filePaths) throws IOException {
		for (FilePath filePath : filePaths) {
			File file = new File(filePath.toAbsolutePath(rootDirectory));
			delete(file);
		}
	}

	private void delete(File file) throws IOException {
		if (!file.exists()) {
			return;
		}
		if (file.isFile()) {
			Files.delete(file.toPath());
			return;
		}
		if (file.isDirectory()) {
			try (DirectoryStream<Path> paths = Files.newDirectoryStream(file.toPath())) {
				for (Path path : paths) {
					delete(path.toFile());
				}
			}
		}
	}

}
