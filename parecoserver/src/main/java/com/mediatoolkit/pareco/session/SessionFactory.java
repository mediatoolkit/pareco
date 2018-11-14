package com.mediatoolkit.pareco.session;

import com.mediatoolkit.pareco.components.DirectoryStructureReader;
import com.mediatoolkit.pareco.components.DirectoryWriter;
import com.mediatoolkit.pareco.components.FileChunkWriter;
import com.mediatoolkit.pareco.components.FileDeleter;
import com.mediatoolkit.pareco.components.FileDigestCalculator;
import com.mediatoolkit.pareco.components.MetadataWriter;
import com.mediatoolkit.pareco.model.DirectoryStructure;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 29/10/2018
 */
@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class SessionFactory {

	private final DirectoryStructureReader directoryStructureReader;
	private final DirectoryWriter directoryWriter;
	private final FileDeleter fileDeleter;
	private final FileDigestCalculator fileDigestCalculator;
	private final FileChunkWriter fileChunkWriter;
	private final MetadataWriter metadataWriter;

	public DownloadSession newDownloadSession(
		String id, String rootDirectory, long chunkSizeBytes, String include, String exclude
	) throws IOException {
		return new DownloadSession(
			directoryStructureReader,
			fileDigestCalculator,
			id,
			rootDirectory,
			chunkSizeBytes,
			include,
			exclude
		);
	}

	public UploadSession newUploadSession(
		String id, String rootDirectory, DirectoryStructure directoryStructure,
		long chunkSizeBytes, String include, String exclude
	) {
		return new UploadSession(
			directoryStructureReader,
			directoryWriter,
			fileDeleter,
			fileDigestCalculator,
			fileChunkWriter,
			metadataWriter,
			id,
			rootDirectory,
			directoryStructure,
			chunkSizeBytes,
			include,
			exclude
		);
	}


}
