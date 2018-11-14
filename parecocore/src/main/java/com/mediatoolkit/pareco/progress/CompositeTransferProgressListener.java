package com.mediatoolkit.pareco.progress;

import com.mediatoolkit.pareco.model.ChunkInfo;
import com.mediatoolkit.pareco.model.DirectoryStructure;
import com.mediatoolkit.pareco.model.FilePath;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import one.util.streamex.StreamEx;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 30/10/2018
 */
@AllArgsConstructor
public class CompositeTransferProgressListener implements TransferProgressListener {

	private List<TransferProgressListener> listeners;

	public static TransferProgressListener of(
		TransferProgressListener... listeners
	) {
		return of(Arrays.asList(listeners));
	}

	public static TransferProgressListener of(
		List<TransferProgressListener> listeners
	) {
		listeners = StreamEx.of(listeners)
			.nonNull()
			.remove(TransferProgressListener.NO_OP_LISTENER::equals)
			.toList();
		if (listeners.isEmpty()) {
			return TransferProgressListener.NO_OP_LISTENER;
		} else if (listeners.size() == 1) {
			return listeners.get(0);
		} else {
			return new CompositeTransferProgressListener(listeners);
		}
	}

	@Override
	public void initializing(String transferMode, String sourceRootDir, String destinationRootDir) {
		listeners.forEach(listener -> listener.initializing(transferMode, sourceRootDir, destinationRootDir));
	}

	@Override
	public void started(DirectoryStructure directoryStructure, long chunkSizeBytes) {
		listeners.forEach(listener -> listener.started(directoryStructure, chunkSizeBytes));
	}

	@Override
	public void deletedFiles(List<FilePath> filePaths) {
		listeners.forEach(listener -> listener.deletedFiles(filePaths));
	}

	@Override
	public void deletedDirectories(List<FilePath> filePaths) {
		listeners.forEach(listener -> listener.deletedDirectories(filePaths));
	}

	@Override
	public void fileAnalyze(FilePath filePath) {
		listeners.forEach(listener -> listener.fileAnalyze(filePath));
	}

	@Override
	public void fileSkipped(FilePath filePath) {
		listeners.forEach(listener -> listener.fileSkipped(filePath));
	}

	@Override
	public void fileStarted(FilePath filePath) {
		listeners.forEach(listener -> listener.fileStarted(filePath));
	}

	@Override
	public void fileCompleted(FilePath filePath) {
		listeners.forEach(listener -> listener.fileCompleted(filePath));
	}

	@Override
	public void fileChunkSkipped(FilePath filePath, ChunkInfo chunkInfo) {
		listeners.forEach(listener -> listener.fileChunkSkipped(filePath, chunkInfo));
	}

	@Override
	public void fileChunkTransferProgress(FilePath filePath, ChunkInfo chunkInfo, long bytesTransferred) {
		listeners.forEach(listener -> listener.fileChunkTransferProgress(filePath, chunkInfo, bytesTransferred));
	}

	@Override
	public void fileChunkTransferred(FilePath filePath, ChunkInfo chunkInfo) {
		listeners.forEach(listener -> listener.fileChunkTransferred(filePath, chunkInfo));
	}

	@Override
	public void completed() {
		listeners.forEach(TransferProgressListener::completed);
	}

	@Override
	public void aborted() {
		listeners.forEach(TransferProgressListener::aborted);
	}

}
