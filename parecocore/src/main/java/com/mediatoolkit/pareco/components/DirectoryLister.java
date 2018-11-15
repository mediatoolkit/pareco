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

	/**
	 * Scan contents of given {@code rootDirectory} to recursively find all matching files and directories.
	 * Only directories and regular files will be selected. (symlinks and named pipes will not)
	 *
	 * @param rootDirectory as source of files and directories
	 * @param filter to use for specific inclusion/exclusion of files and/or directories. If some directory is
	 *               explicitly included then all of its child files and directories will be included (regardless
	 *               if some sub file/dire is excluded). If some directory is excluded then all of its contents will
	 *               be excluded too.
	 * @return a list of {@link FileEntry}-es which represent all files and folders found
	 * @throws IOException if some error occurs such as read permissions, external deletion, ...
	 * @throws NotDirectoryException if {@code rootDirectory} does not exist or it is not a directory
	 */
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
