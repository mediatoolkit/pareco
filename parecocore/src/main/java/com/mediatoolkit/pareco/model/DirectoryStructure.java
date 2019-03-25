package com.mediatoolkit.pareco.model;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Singular;
import one.util.streamex.StreamEx;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 24/10/2018
 */
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
@Builder
public class DirectoryStructure {

	@Singular
	private List<DirectoryMetadata> directories;
	@Singular
	private List<FileMetadata> files;

	public List<FilePath> listDirectories() {
		return StreamEx.of(directories).map(DirectoryMetadata::getFilePath).toList();
	}

	public List<FilePath> listFiles() {
		return StreamEx.of(files).map(FileMetadata::getFilePath).toList();
	}

	public Map<FilePath, DirectoryMetadata> directoryMetadataAsMap() {
		return StreamEx.of(directories)
			.mapToEntry(DirectoryMetadata::getFilePath, Function.identity())
			.toMap();
	}

	public Map<FilePath, FileMetadata> filesMetadataAsMap() {
		return StreamEx.of(files)
			.mapToEntry(FileMetadata::getFilePath, Function.identity())
			.toMap();
	}

	public long getTotalSizeBytes() {
		int dirsSize = directories.size() * 4096;
		long filesSize = StreamEx.of(files)
			.mapToLong(FileMetadata::getFileSizeBytes)
			.sum();
		return dirsSize + filesSize;
	}

}
