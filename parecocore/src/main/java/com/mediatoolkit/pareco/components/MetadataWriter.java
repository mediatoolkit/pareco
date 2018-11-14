package com.mediatoolkit.pareco.components;

import com.mediatoolkit.pareco.model.DirectoryMetadata;
import com.mediatoolkit.pareco.model.FileMetadata;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 24/10/2018
 */
@Component
public class MetadataWriter {

	public void writeFileMetadata(
		String rootDirectory,
		FileMetadata fileMetadata
	) throws IOException {
		File file = new File(fileMetadata.getFilePath().toAbsolutePath(rootDirectory));
		Path path = file.toPath();
		if (fileMetadata.getPermissions() != null) {
			try {
				Files.setPosixFilePermissions(path, fileMetadata.getPermissions());
			} catch (UnsupportedOperationException ignore) {
			}
		}
		file.setLastModified(fileMetadata.getLastModifiedTimeMillis());
		long fileLength = file.length();
		if (fileLength != fileMetadata.getFileSizeBytes()) {
			throw new IllegalStateException(String.format(
				"File: '%s', expectedSize:%d actualSize:%d",
				path, fileMetadata.getFileSizeBytes(), fileLength
			));
		}
	}

	public void writeDirectoryMetadata(
		String rootDirectory,
		DirectoryMetadata directoryMetadata
	) throws IOException {
		File file = new File(directoryMetadata.getFilePath().toAbsolutePath(rootDirectory));
		Path path = file.toPath();
		if (directoryMetadata.getPermissions() != null) {
			try {
				Files.setPosixFilePermissions(path, directoryMetadata.getPermissions());
			} catch (UnsupportedOperationException ignore) {
			}
		}
	}

}
