package com.mediatoolkit.pareco.progress;

import com.mediatoolkit.pareco.model.ChunkInfo;
import com.mediatoolkit.pareco.model.DirectoryStructure;
import com.mediatoolkit.pareco.model.FileMetadata;
import com.mediatoolkit.pareco.model.FilePath;
import com.mediatoolkit.pareco.progress.StatsRecordingTransferProgressListener.FileTransferStats;
import com.mediatoolkit.pareco.progress.log.LoggingAppender;
import com.mediatoolkit.pareco.progress.log.Message;
import com.mediatoolkit.pareco.progress.log.Message.Format;
import static com.mediatoolkit.pareco.progress.log.Message.highlight;
import static com.mediatoolkit.pareco.progress.log.Message.msg;
import static com.mediatoolkit.pareco.progress.log.Message.quote;
import static com.mediatoolkit.pareco.util.Util.divRound1d;
import static com.mediatoolkit.pareco.util.Util.durationPretty;
import static com.mediatoolkit.pareco.util.Util.fileSizePretty;
import static com.mediatoolkit.pareco.util.Util.round1d;
import static java.lang.String.format;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;


/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 30/10/2018
 */
public class LoggingTransferProgressListener implements TransferProgressListener {

	private final StatsRecordingTransferProgressListener statsListener;
	private final Speedometer speedometer;
	private final LoggingFilter loggingFilter;
	private final LoggingAppender loggingAppender;

	private Map<FilePath, FileMetadata> fileMetadataMap;
	private long chunkSizeBytes;

	private long lastSpeedPrintTime = 0;

	@Value
	public static class LoggingFilter {

		private boolean startEnd;
		private boolean files;
		private boolean chunks;
		private boolean speed;

		public static final LoggingFilter START_END = new LoggingFilter(true, false, false, false);
		public static final LoggingFilter START_END_SPEED = new LoggingFilter(true, false, false, true);
		public static final LoggingFilter FILES = new LoggingFilter(true, true, false, false);
		public static final LoggingFilter FILES_SPEED = new LoggingFilter(true, true, false, true);
		public static final LoggingFilter CHUNKS = new LoggingFilter(true, true, true, true);
	}

	private void logTransfer(Object... parts) {
		Message msg = msg(Format.NONE, parts);
		loggingAppender.addMsg("TRANSFER_TASK", msg);
	}

	private void logDelete(Object... parts) {
		Message msg = msg(Format.DELETE, parts);
		loggingAppender.addMsg("DELETE", msg);
	}

	private void logFile(Object... parts) {
		Message msg = msg(Format.FILE, parts);
		loggingAppender.addMsg("FILE", msg);
	}

	private void logChunk(Object... parts) {
		Message msg = msg(Format.CHUNK, parts);
		loggingAppender.addMsg("CHUNK", msg);
	}

	private void logSpeed(Object... parts) {
		Message msg = msg(Format.SPEED, parts);
		loggingAppender.addMsg("SPEED", msg);
	}

	@Builder
	private LoggingTransferProgressListener(
		@NonNull LoggingFilter loggingFilter,
		@NonNull LoggingAppender loggingAppender,
		StatsRecordingTransferProgressListener statsListener,
		Speedometer speedometer
	) {
		this.statsListener = statsListener;
		this.speedometer = speedometer;
		this.loggingFilter = loggingFilter;
		this.loggingAppender = loggingAppender;
	}

	private String fileRank(FilePath filePath) {
		if (statsListener == null) {
			return null;
		}
		Integer fileRank = statsListener.getFileRank(filePath);
		return (fileRank + 1) + "/" + statsListener.getNumFiles();
	}

	private double totalProgressPercent() {
		long bytesCompleted = statsListener.getTransferredBytes() + statsListener.getSkippedBytes();
		long bytesTotal = statsListener.getTotalSize();
		return round1d(100. * bytesCompleted / bytesTotal);
	}

	private String chunkRank(FilePath filePath, ChunkInfo chunkInfo) {
		long fileSizeBytes = fileMetadataMap.get(filePath).getFileSizeBytes();
		int numChunks = (int) ((fileSizeBytes + chunkSizeBytes - 1) / chunkSizeBytes);
		int chunkRank = (int) (chunkInfo.getOffsetBytes() / chunkSizeBytes);
		return (chunkRank + 1) + "/" + numChunks;
	}

	private Duration etaDuration(double transferSpeed) {
		if (transferSpeed <= 0) {
			return null;
		}
		long bytesLeft = statsListener.getTotalSize()
			- statsListener.getTransferredBytes()
			- statsListener.getSkippedBytes();
		double secondsLeft = bytesLeft / transferSpeed;

		double percentSkipped = (double) statsListener.getSkippedBytes() / statsListener.getTotalSize();
		double expectedRawTransferMillisLeft = (1 - percentSkipped) * secondsLeft * 1000;
		double expectedSkippingMillis = (1 - percentSkipped) * statsListener.timeFromStartToNow();
		return Duration.ofMillis((long) (expectedRawTransferMillisLeft + expectedSkippingMillis));
	}

	@Override
	public void initializing(
		String transferMode,
		String sourceRootDir, String destinationRootDir,
		String serverUrl
	) {
		logTransfer("Initializing ", highlight(transferMode), " transfer");
		logTransfer("  - Source dir: ", quote(sourceRootDir));
		logTransfer("  - Destination dir: ", quote(destinationRootDir));
		logTransfer("Server ", quote(serverUrl), " connecting...");
	}

	@Override
	public void analyzingFiles(String sourceRootDir, String destinationRootDir) {
		logTransfer("Analyzing contents of directories...");
	}

	@Override
	public void started(DirectoryStructure directoryStructure, long chunkSizeBytes) {
		this.chunkSizeBytes = chunkSizeBytes;
		this.fileMetadataMap = directoryStructure.filesMetadataAsMap();
		if (!loggingFilter.isStartEnd()) {
			return;
		}
		logTransfer("--------------------------------------------");
		if (statsListener == null) {
			logTransfer("Transfer task started");
		} else {
			logTransfer("Transfer task started, stats:");
			logTransfer("  - Analysis duration: ", durationPretty(Duration.ofMillis(statsListener.analyzeTotalTime())));
			logTransfer("  - Num directories: ", statsListener.getNumDirectories());
			logTransfer("  - Num files: ", statsListener.getNumFiles());
			logTransfer("  - Total size: ", fileSizePretty(statsListener.getTotalSize()));
		}
		logTransfer("--------------------------------------------");
	}

	@Override
	public void deletedFiles(List<FilePath> filePaths) {
		if (!loggingFilter.isFiles()) {
			return;
		}
		filePaths.forEach(filePath -> logDelete("Deleted unexpected file: ", quote(filePath)));
		logDelete("Deleted files: ", filePaths.size());
	}

	@Override
	public void deletedDirectories(List<FilePath> filePaths) {
		if (!loggingFilter.isFiles()) {
			return;
		}
		filePaths.forEach(filePath -> logDelete("Deleted unexpected directory: ", quote(filePath)));
		logDelete("Deleted directories: ", filePaths.size());
	}

	@Override
	public void fileAnalyze(FilePath filePath) {
		if (!loggingFilter.isFiles()) {
			return;
		}
		long fileSize = fileMetadataMap.get(filePath).getFileSizeBytes();
		if (statsListener == null) {
			logFile(format("[%s] | Analyzing file: ", fileSizePretty(fileSize)), quote(filePath));
		}
		logFile(format("Total: %s%% | (%s) [%s] | Analyzing file: ",
			totalProgressPercent(), fileRank(filePath), fileSizePretty(fileSize)),
			quote(filePath)
		);
	}

	@Override
	public void fileSkipped(FilePath filePath) {
		if (!loggingFilter.isFiles()) {
			return;
		}
		long fileSize = fileMetadataMap.get(filePath).getFileSizeBytes();
		if (statsListener == null) {
			logFile(format("[%s] | Skipping file: ", fileSizePretty(fileSize)), quote(filePath));
		}
		logFile(format("Total: %s%% | (%s) [%s] | Skipping file: ",
			totalProgressPercent(), fileRank(filePath), fileSizePretty(fileSize)),
			quote(filePath)
		);
	}

	@Override
	public void fileStarted(FilePath filePath) {
		if (!loggingFilter.isFiles()) {
			return;
		}
		long fileSize = fileMetadataMap.get(filePath).getFileSizeBytes();
		if (statsListener == null) {
			logFile(format("[%s] | Start transfer file: ", fileSizePretty(fileSize)), quote(filePath));
		}
		logFile(format("Total: %s%% | (%s) [%s] | Start transfer file: ",
			totalProgressPercent(), fileRank(filePath), fileSizePretty(fileSize)),
			quote(filePath)
		);
	}

	@Override
	public void fileDeleted(FilePath filePath) {
		if (!loggingFilter.isFiles()) {
			return;
		}
		long fileSize = fileMetadataMap.get(filePath).getFileSizeBytes();
		logFile("[", fileSizePretty(fileSize), "] | ",
			"Deleted file because ", highlight("source file"), " is ", highlight("DELETED"), " ", quote(filePath)
		);
	}

	@Override
	public void fileCompleted(FilePath filePath) {
		if (!loggingFilter.isFiles()) {
			return;
		}
		long fileSize = fileMetadataMap.get(filePath).getFileSizeBytes();
		if (statsListener == null) {
			logFile("[", fileSizePretty(fileSize), "] | File done ", quote(filePath));
			return;
		}
		FileTransferStats fileStats = statsListener.getFileStats(filePath);
		double effectiveSpeed = divRound1d(1000 * fileSize, fileStats.totalTime());
		double percentSkipped = round1d(100. * fileStats.getSkippedBytes() / fileSize);
		logFile(format("Total: %s%% | (%s) [%s] | File done in %s, speed %s, skip: %s ",
			totalProgressPercent(), fileRank(filePath), fileSizePretty(fileSize),
			durationPretty(Duration.ofMillis(fileStats.totalTime())),
			fileSizePretty(effectiveSpeed) + "/s",
			percentSkipped + "%"),
			quote(filePath)
		);
	}

	@Override
	public void fileChunkSkipped(FilePath filePath, ChunkInfo chunkInfo) {
		if (!loggingFilter.isChunks()) {
			return;
		}
		long fileSize = fileMetadataMap.get(filePath).getFileSizeBytes();
		String chunkRank = chunkRank(filePath, chunkInfo);
		if (statsListener == null) {
			logChunk(format("[%s] | (%s) [%s] | Skipping chunk in file: ",
				fileSizePretty(fileSize), chunkRank, fileSizePretty(chunkInfo.getSizeBytes())),
				quote(filePath)
			);
			return;
		}
		logChunk(format("Total: %s%% | (%s) [%s] | (%s) [%s] | Skipping chunk in file: ",
			totalProgressPercent(), fileRank(filePath), fileSizePretty(fileSize),
			chunkRank, fileSizePretty(chunkInfo.getSizeBytes())),
			quote(filePath)
		);
	}

	@Override
	public void fileChunkTransferProgress(FilePath filePath, ChunkInfo chunkInfo, long bytesTransferred) {
		if (!loggingFilter.isSpeed()) {
			return;
		}
		if (speedometer == null) {
			return;
		}
		speedometer.increment(bytesTransferred);
		long now = System.currentTimeMillis();
		if (acquirePrintSpeed(now)) {
			double speed = round1d(speedometer.getSpeed());
			if (statsListener == null) {
				logSpeed("Speed: ", fileSizePretty(speed) + "/s");
			} else {
				Duration eta = etaDuration(speed);
				String etaPretty = eta == null ? "--:--" : durationPretty(eta);
				logSpeed(format("Total: %s%% | Speed: %s ETA: %s",
					totalProgressPercent(),
					fileSizePretty(speed) + "/s",
					etaPretty
				));
			}
		}
	}

	private synchronized boolean acquirePrintSpeed(long now) {
		if (now - lastSpeedPrintTime > 1000) {
			lastSpeedPrintTime = now;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void fileChunkTransferred(FilePath filePath, ChunkInfo chunkInfo) {
		if (!loggingFilter.isChunks()) {
			return;
		}
		long fileSize = fileMetadataMap.get(filePath).getFileSizeBytes();
		String chunkRank = chunkRank(filePath, chunkInfo);
		if (statsListener == null) {
			logChunk(format("[%s] | (%s) [%s] | Completed chunk in file: ",
				fileSizePretty(fileSize), chunkRank, fileSizePretty(chunkInfo.getSizeBytes())),
				quote(filePath)
			);
			return;
		}
		logChunk(format("Total: %s%% | (%s) [%s] | (%s) [%s] | Completed chunk in file: ",
			totalProgressPercent(), fileRank(filePath), fileSizePretty(fileSize),
			chunkRank, fileSizePretty(chunkInfo.getSizeBytes())),
			quote(filePath)
		);
	}

	@Override
	public void completed() {
		if (!loggingFilter.isStartEnd()) {
			return;
		}
		if (statsListener == null) {
			logTransfer("--------------------------------------------");
			logTransfer("Transfer task is ", highlight("completed"));
			logTransfer("--------------------------------------------");
			return;
		}
		String totalDuration = durationPretty(Duration.ofMillis(statsListener.totalTime()));
		long totalSize = statsListener.getTotalSize();
		String totalSizePretty = fileSizePretty(totalSize);
		String skippedSize = fileSizePretty(statsListener.getSkippedBytes());
		double skippedSizePercent = divRound1d(100 * statsListener.getSkippedBytes(), totalSize);
		String transferredSize = fileSizePretty(statsListener.getTransferredBytes());
		double transferredSizePercent = divRound1d(100 * statsListener.getTransferredBytes(), totalSize);
		int numDirectories = statsListener.getNumDirectories();
		int numFiles = statsListener.getNumFiles();
		int numSkippedFiles = statsListener.getSkippedFiles();
		int numTransferredFiles = statsListener.getTransferredFiles();
		double numSkippedPercent = divRound1d(100 * numSkippedFiles, numFiles);
		double numTransferredPercent = divRound1d(100 * numTransferredFiles, numFiles);
		double avgSpeed = divRound1d(1000 * statsListener.getTransferredBytes(), statsListener.totalTime());
		double effectiveAvgSpeed = divRound1d(1000 * totalSize, statsListener.totalTime());
		int numSkippedFilesChunks = statsListener.getSkippedFilesChunks();
		int numTransferredFilesChunks = statsListener.getTransferredChunks() + statsListener.getSkippedChunks();
		double skippedChunksPercent = divRound1d(100 * statsListener.getSkippedChunks(), numTransferredFilesChunks);
		double transferredChunksPercent = divRound1d(100 * statsListener.getTransferredChunks(), numTransferredFilesChunks);
		logTransfer("--------------------------------------------");
		logTransfer("Transfer task is ", highlight("completed"), ", stats:");
		logTransfer("  - Total duration: ", totalDuration);
		logTransfer("  - Total size: ", totalSizePretty);
		logTransfer("     - Skipped: ", skippedSize, ", ", skippedSizePercent + "%");
		logTransfer("     - Transferred: ", transferredSize, ", ", transferredSizePercent + "%");
		logTransfer("  - Num directories: ", numDirectories);
		logTransfer("     - Deleted: ", statsListener.getDeletedDirectories());
		logTransfer("  - Num files: ", numFiles);
		logTransfer("     - Skipped: ", numSkippedFiles, ", ", numSkippedPercent + "%");
		logTransfer("         - Num chunks: ", numSkippedFilesChunks);
		logTransfer("     - Transferred: ", numTransferredFiles, ", ", numTransferredPercent + "%");
		logTransfer("         - Num chunks: ", numTransferredFilesChunks);
		logTransfer("             - Skipped: ", statsListener.getSkippedChunks(), ", ", skippedChunksPercent + "%");
		logTransfer("             - Transferred: ", statsListener.getTransferredChunks(), ", ", transferredChunksPercent + "%");
		logTransfer("     - Deleted: ", statsListener.getDeletedFiles());
		logTransfer("     - Concurrent deletions: ", statsListener.getConcurrentDeletions());
		logTransfer("  - Speed:");
		logTransfer("     - Average: ", fileSizePretty(avgSpeed) + "/s");
		logTransfer("     - Effective: ", fileSizePretty(effectiveAvgSpeed) + "/s");
		logTransfer("--------------------------------------------");
	}

	@Override
	public void aborted() {
		if (!loggingFilter.isStartEnd()) {
			return;
		}
		logTransfer("--------------------------------------------");
		if (statsListener == null) {
			logTransfer("Transfer task is ", highlight("aborted"));
		} else {
			String totalDuration = durationPretty(Duration.ofMillis(statsListener.totalTime()));
			logTransfer("Transfer task is ", highlight("aborted"),
				", transfer ", totalProgressPercent(), "% duration: ", totalDuration
			);
		}
		logTransfer("--------------------------------------------");
	}
}
