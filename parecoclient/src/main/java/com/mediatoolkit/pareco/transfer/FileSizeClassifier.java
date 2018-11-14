package com.mediatoolkit.pareco.transfer;

import com.mediatoolkit.pareco.model.FileMetadata;
import com.mediatoolkit.pareco.transfer.model.SizeClassifiedFiles;
import com.mediatoolkit.pareco.transfer.model.SizeClassifiedFiles.SizeClassifiedFilesBuilder;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 01/11/2018
 */
@Component
public class FileSizeClassifier {

	public SizeClassifiedFiles classifySmallAndBigFiles(
		List<FileMetadata> files, long chunkSizeBytes, int numTransferConnections
	) {
		long smallFileThresholdSize = chunkSizeBytes * numTransferConnections;
		SizeClassifiedFilesBuilder builder = SizeClassifiedFiles.builder();
		for (FileMetadata file : files) {
			if (file.getFileSizeBytes() < smallFileThresholdSize) {
				builder.smallFile(file);
			} else {
				builder.bigFile(file);
			}
		}
		return builder.build();
	}
}
