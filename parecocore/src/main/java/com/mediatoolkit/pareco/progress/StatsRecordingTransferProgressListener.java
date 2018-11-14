package com.mediatoolkit.pareco.progress;

import com.mediatoolkit.pareco.model.ChunkInfo;
import com.mediatoolkit.pareco.model.DirectoryStructure;
import com.mediatoolkit.pareco.model.FileMetadata;
import com.mediatoolkit.pareco.model.FilePath;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.Getter;
import lombok.Synchronized;
import one.util.streamex.StreamEx;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 30/10/2018
 */
@Getter
public class StatsRecordingTransferProgressListener implements TransferProgressListener {

	private DirectoryStructure directoryStructure;
	private Map<FilePath, FileMetadata> fileMetadataMap;
	private long chunkSizeBytes;
	private long timeStart;
	private long timeEnd;
	private long skippedBytes;
	private long transferredBytes;
	private int skippedFiles;
	private int transferredFiles;
	private long totalSize;
	private int deletedFiles;
	private int deletedDirectories;
	private int numFiles;
	private int numDirectories;
	private int skippedFilesChunks;
	private int transferredChunks;
	private int skippedChunks;

	private Map<FilePath, FileTransferStats> filesStats = new HashMap<>();
	private int currentFileRank = 0;
	private Map<FilePath, Integer> filesRanks = new HashMap<>();

	public long totalTime() {
		return timeEnd - timeStart;
	}

	public FileTransferStats getFileStats(FilePath filePath) {
		return filesStats.get(filePath);
	}

	public Integer getFileRank(FilePath filePath) {
		return filesRanks.get(filePath);
	}

	@Override
	public void initializing(String transferMode, String sourceRootDir, String destinationRootDir) {
		timeStart = System.currentTimeMillis();
	}

	@Override
	public void started(DirectoryStructure directoryStructure, long chunkSizeBytes) {
		this.directoryStructure = directoryStructure;
		this.chunkSizeBytes = chunkSizeBytes;
		fileMetadataMap = directoryStructure.filesMetadataAsMap();
		numFiles = directoryStructure.getFiles().size();
		numDirectories = directoryStructure.getDirectories().size();
		totalSize = StreamEx.ofValues(fileMetadataMap)
			.mapToLong(FileMetadata::getFileSizeBytes)
			.sum();
	}

	@Override
	public void deletedFiles(List<FilePath> filePaths) {
		deletedFiles += filePaths.size();
	}

	@Override
	public void deletedDirectories(List<FilePath> filePaths) {
		deletedDirectories += filePaths.size();
	}

	@Override
	@Synchronized
	public void fileAnalyze(FilePath filePath) {
		filesRanks.put(filePath, currentFileRank);
		currentFileRank++;
	}

	@Override
	@Synchronized
	public void fileSkipped(FilePath filePath) {
		skippedFiles++;
		FileMetadata fileMetadata = fileMetadataMap.get(filePath);
		skippedBytes += fileMetadata.getFileSizeBytes();
		int numChunks = (int) ((fileMetadata.getFileSizeBytes() + chunkSizeBytes - 1) / chunkSizeBytes);
		skippedFilesChunks += numChunks;
	}

	@Override
	@Synchronized
	public void fileStarted(FilePath filePath) {
		FileTransferStats fileStats = new FileTransferStats();
		filesStats.put(filePath, fileStats);
		fileStats.timeStart = System.currentTimeMillis();
	}

	@Override
	@Synchronized
	public void fileCompleted(FilePath filePath) {
		transferredFiles++;
		FileTransferStats fileStats = filesStats.get(filePath);
		fileStats.timeEnd = System.currentTimeMillis();
	}

	@Override
	@Synchronized
	public void fileChunkSkipped(FilePath filePath, ChunkInfo chunkInfo) {
		FileTransferStats fileStats = filesStats.get(filePath);
		skippedChunks++;
		fileStats.skippedChunks++;
		fileStats.skippedBytes += chunkInfo.getSizeBytes();
		skippedBytes += chunkInfo.getSizeBytes();
	}

	@Override
	@Synchronized
	public void fileChunkTransferProgress(FilePath filePath, ChunkInfo chunkInfo, long bytesTransferred) {
		FileTransferStats fileStats = filesStats.get(filePath);
		fileStats.transferredBytes += bytesTransferred;
		transferredBytes += bytesTransferred;
	}

	@Override
	@Synchronized
	public void fileChunkTransferred(FilePath filePath, ChunkInfo chunkInfo) {
		FileTransferStats fileStats = filesStats.get(filePath);
		fileStats.transferredChunks++;
		transferredChunks++;
	}

	@Override
	public void completed() {
		timeEnd = System.currentTimeMillis();
	}

	@Override
	public void aborted() {
		timeEnd = System.currentTimeMillis();
	}

	@Data
	public static class FileTransferStats {

		private long timeStart;
		private long timeEnd;

		private int skippedChunks;
		private int transferredChunks;

		private long skippedBytes;
		private long transferredBytes;

		public long totalTime() {
			return timeEnd - timeStart;
		}
	}

}
