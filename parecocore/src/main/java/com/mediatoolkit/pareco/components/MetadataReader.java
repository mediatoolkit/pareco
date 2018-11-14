package com.mediatoolkit.pareco.components;

import com.mediatoolkit.pareco.model.DirectoryMetadata;
import com.mediatoolkit.pareco.model.FileMetadata;
import com.mediatoolkit.pareco.model.FilePath;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 24/10/2018
 */
@Component
public class MetadataReader {

	public FileMetadata readFileMetadata(
		String rootDirectory,
		FilePath filePath
	) throws IOException {
		Path path = new File(filePath.toAbsolutePath(rootDirectory)).toPath();
		BasicFileAttributes fileAttributes = Files.readAttributes(path, BasicFileAttributes.class);
		long lastModifiedTimeMillis = fileAttributes.lastModifiedTime().toMillis();
		long sizeBytes = fileAttributes.size();
		Set<PosixFilePermission> posixFilePermissions;
		try {
			posixFilePermissions = Files.getPosixFilePermissions(path);
		} catch (UnsupportedOperationException ignore) {
			posixFilePermissions = null;
		}
		return FileMetadata.builder()
			.filePath(filePath)
			.fileSizeBytes(sizeBytes)
			.lastModifiedTimeMillis(lastModifiedTimeMillis)
			.permissions(posixFilePermissions)
			.build();
	}

	public DirectoryMetadata readDirectoryMetadata(
		String rootDirectory,
		FilePath dirPath
	) throws IOException {
		Path path = new File(dirPath.toAbsolutePath(rootDirectory)).toPath();
		Set<PosixFilePermission> posixFilePermissions;
		try {
			posixFilePermissions = Files.getPosixFilePermissions(path);
		} catch (UnsupportedOperationException ignore) {
			posixFilePermissions = null;
		}
		return DirectoryMetadata.builder()
			.filePath(dirPath)
			.permissions(posixFilePermissions)
			.build();
	}
}
