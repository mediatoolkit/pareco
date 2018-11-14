package com.mediatoolkit.pareco.components;

import com.mediatoolkit.pareco.model.FilePath;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 31/10/2018
 */
@Component
public class FileDeleter {

	public void delete(String rootDirectory, List<FilePath> filePaths) throws IOException {
		for (FilePath filePath : filePaths) {
			delete(rootDirectory, filePath);
		}
	}

	public void delete(String rootDirectory, FilePath filePath) throws IOException {
		File file = new File(filePath.toAbsolutePath(rootDirectory));
		Files.delete(file.toPath());
	}
}
