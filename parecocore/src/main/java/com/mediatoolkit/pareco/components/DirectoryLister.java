package com.mediatoolkit.pareco.components;

import com.mediatoolkit.pareco.exceptions.NotDirectoryException;
import com.mediatoolkit.pareco.model.FileEntry;
import com.mediatoolkit.pareco.model.FilePath;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 24/10/2018
 */
@Component
public class DirectoryLister {

	public List<FileEntry> listFiles(String rootDirectory, FileFilter filter) throws IOException {
		File root = new File(rootDirectory);
		if (!root.exists()) {
			throw new NotDirectoryException("Given dir pathname does not exist: " + rootDirectory);
		}
		if (!root.isDirectory()) {
			throw new NotDirectoryException("Given pathname is not a directory: " + rootDirectory);
		}
		List<FileEntry> files = getChildFiles("", root, filter);
		return files;
	}

	private List<FileEntry> getChildFiles(
		String relativeDirectory, File root, FileFilter filter
	) throws IOException {
		List<FileEntry> files = new ArrayList<>();
		try (DirectoryStream<Path> paths = Files.newDirectoryStream(root.toPath())) {
			for (Path path : paths) {
				if (filter.excludes(path)) {
					continue;
				}
				boolean includes = filter.includes(path);
				File file = path.toFile();
				FilePath filePath = FilePath.of(relativeDirectory, file.getName());
				if (file.isDirectory()) {
					String dirName = file.getName();
					String subDirectory = relativeDirectory.isEmpty() ? dirName : relativeDirectory + File.separator + dirName;
					FileFilter subFilter = includes ? filter.withIncludeAll() : filter;
					List<FileEntry> subFiles = getChildFiles(subDirectory, file, subFilter);
					if (includes || !subFiles.isEmpty()) {
						files.add(FileEntry.directory(filePath));
						files.addAll(subFiles);
					}
				} else if (includes && Files.isRegularFile(file.toPath())) {
					files.add(FileEntry.file(filePath));
				}
			}
		}
		return files;
	}

}
