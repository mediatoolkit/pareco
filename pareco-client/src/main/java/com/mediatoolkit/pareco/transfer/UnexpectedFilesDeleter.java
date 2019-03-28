package com.mediatoolkit.pareco.transfer;

import com.mediatoolkit.pareco.model.DirectoryStructure;
import com.mediatoolkit.pareco.model.FilePath;
import com.mediatoolkit.pareco.progress.TransferProgressListener;
import com.mediatoolkit.pareco.transfer.model.UnexpectedFiles;
import java.io.IOException;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 01/11/2018
 */
@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class UnexpectedFilesDeleter {

	private final UnexpectedFilesResolver unexpectedFilesResolver;

	public void deleteUnexpected(
		TransferProgressListener progressListener,
		DirectoryStructure sourceDirectoryStructure,
		DirectoryStructure destinationDirectoryStructure,
		DeletePerformer deletePerformer
	) throws IOException {
		UnexpectedFiles unexpectedFiles = unexpectedFilesResolver.resolveUnexpectedFiles(
			sourceDirectoryStructure, destinationDirectoryStructure
		);
		if (!unexpectedFiles.getAll().isEmpty()) {
			deletePerformer.performDeletions(unexpectedFiles.getAll());
		}
		if (!unexpectedFiles.getFiles().isEmpty()) {
			progressListener.deletedFiles(unexpectedFiles.getFiles());
		}
		if (!unexpectedFiles.getDirectories().isEmpty()) {
			progressListener.deletedDirectories(unexpectedFiles.getDirectories());
		}
	}

	public interface DeletePerformer {

		void performDeletions(List<FilePath> filePaths) throws IOException;

	}

}
