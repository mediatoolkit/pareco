package com.mediatoolkit.pareco.progress;

import com.mediatoolkit.pareco.model.ChunkInfo;
import com.mediatoolkit.pareco.model.DirectoryStructure;
import com.mediatoolkit.pareco.model.FilePath;
import java.util.List;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 30/10/2018
 */
public interface TransferProgressListener {

	void initializing(String transferMode, String sourceRootDir, String destinationRootDir);

	void started(DirectoryStructure directoryStructure, long chunkSizeBytes);

	void deletedFiles(List<FilePath> filePaths);

	void deletedDirectories(List<FilePath> filePaths);

	void fileAnalyze(FilePath filePath);

	void fileSkipped(FilePath filePath);

	void fileStarted(FilePath filePath);

	void fileDeleted(FilePath filePath);

	void fileCompleted(FilePath filePath);

	void fileChunkSkipped(FilePath filePath, ChunkInfo chunkInfo);

	void fileChunkTransferProgress(FilePath filePath, ChunkInfo chunkInfo, long bytesTransferred);

	void fileChunkTransferred(FilePath filePath, ChunkInfo chunkInfo);

	void completed();

	void aborted();

	TransferProgressListener NO_OP_LISTENER = new TransferProgressListener() {

		@Override
		public void initializing(String transferMode, String sourceRootDir, String destinationRootDir) {
		}

		@Override
		public void started(DirectoryStructure directoryStructure, long chunkSizeBytes) {
		}

		@Override
		public void deletedFiles(List<FilePath> filePaths) {
		}

		@Override
		public void deletedDirectories(List<FilePath> filePaths) {
		}

		@Override
		public void fileAnalyze(FilePath filePath) {
		}

		@Override
		public void fileSkipped(FilePath filePath) {
		}

		@Override
		public void fileStarted(FilePath filePath) {
		}

		@Override
		public void fileDeleted(FilePath filePath) {
		}

		@Override
		public void fileCompleted(FilePath filePath) {
		}

		@Override
		public void fileChunkSkipped(FilePath filePath, ChunkInfo chunkInfo) {
		}

		@Override
		public void fileChunkTransferProgress(FilePath filePath, ChunkInfo chunkInfo, long bytesTransferred) {
		}

		@Override
		public void fileChunkTransferred(FilePath filePath, ChunkInfo chunkInfo) {
		}

		@Override
		public void completed() {
		}

		@Override
		public void aborted() {
		}
	};

}
