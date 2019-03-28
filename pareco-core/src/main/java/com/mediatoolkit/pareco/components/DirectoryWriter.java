package com.mediatoolkit.pareco.components;

import com.mediatoolkit.pareco.model.DirectoryMetadata;
import java.io.File;
import java.io.IOException;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 25/10/2018
 */
@Slf4j
@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class DirectoryWriter {

	private final MetadataWriter metadataWriter;

	public void createDirectories(
		String rootDirectory, List<DirectoryMetadata> directories
	) throws IOException {
		for (DirectoryMetadata directoryMetadata : directories) {
			createDirectory(rootDirectory, directoryMetadata);
		}
	}

	public void createDirectory(
		String rootDirectory, DirectoryMetadata directoryMetadata
	) throws IOException {
		File directory = new File(directoryMetadata.getFilePath().toAbsolutePath(rootDirectory));
		directory.mkdirs();
		metadataWriter.writeDirectoryMetadata(rootDirectory, directoryMetadata);
	}

}
