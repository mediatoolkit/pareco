package com.mediatoolkit.pareco.transfer;

import com.mediatoolkit.pareco.model.DirectoryStructure;
import com.mediatoolkit.pareco.model.FilePath;
import com.mediatoolkit.pareco.transfer.model.UnexpectedFiles;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import one.util.streamex.StreamEx;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 01/11/2018
 */
@Component
public class UnexpectedFilesResolver {

	public UnexpectedFiles resolveUnexpectedFiles(
		DirectoryStructure sourceDirectoryStructure,
		DirectoryStructure destinationDirectoryStructure
	) {
		List<FilePath> sourceFiles = sourceDirectoryStructure.listFiles();
		List<FilePath> destinationFiles = destinationDirectoryStructure.listFiles();
		Set<FilePath> sourceFilesSet = new HashSet<>(sourceFiles);
		List<FilePath> unexpectedFiles = StreamEx.of(destinationFiles)
			.remove(sourceFilesSet::contains)
			.toList();
		List<FilePath> sourceDirectories = sourceDirectoryStructure.listDirectories();
		List<FilePath> destinationDirectories = destinationDirectoryStructure.listDirectories();
		Set<FilePath> sourceDirectoriesSet = new HashSet<>(sourceDirectories);
		List<FilePath> unexpectedDirectories = StreamEx.of(destinationDirectories)
			.remove(sourceDirectoriesSet::contains)
			.toList();
		List<FilePath> filePathsToDelete = StreamEx.<FilePath>empty()
			.append(unexpectedFiles)
			.append(unexpectedDirectories)
			.toList();
		return new UnexpectedFiles(
			unexpectedFiles, unexpectedDirectories, filePathsToDelete
		);
	}

}
