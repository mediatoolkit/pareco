package com.mediatoolkit.pareco.components;

import com.mediatoolkit.pareco.model.DirectoryMetadata;
import com.mediatoolkit.pareco.model.DirectoryStructure;
import com.mediatoolkit.pareco.model.DirectoryStructure.DirectoryStructureBuilder;
import com.mediatoolkit.pareco.model.FileEntry;
import com.mediatoolkit.pareco.model.FileMetadata;
import com.mediatoolkit.pareco.model.FilePath;
import java.io.IOException;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 25/10/2018
 */
@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class DirectoryStructureReader {

	private final DirectoryLister directoryLister;
	private final MetadataReader metadataReader;

	public DirectoryStructure readDirectoryStructure(
		String rootDirectory, String include, String exclude
	) throws IOException {
		FileFilter filter = FileFilter.of(rootDirectory, include, exclude);
		List<FileEntry> allFilesEntries = directoryLister.listFiles(rootDirectory, filter);
		DirectoryStructureBuilder structureBuilder = DirectoryStructure.builder();
		for (FileEntry fileEntry : allFilesEntries) {
			FilePath filePath = fileEntry.getFilePath();
			if (fileEntry.isDirectory()) {
				DirectoryMetadata directoryMetadata = metadataReader.readDirectoryMetadata(rootDirectory, filePath);
				structureBuilder.directory(directoryMetadata);
			} else {
				FileMetadata fileMetadata = metadataReader.readFileMetadata(rootDirectory, filePath);
				structureBuilder.file(fileMetadata);
			}
		}
		return structureBuilder.build();
	}
}
