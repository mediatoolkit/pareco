package com.mediatoolkit.pareco.progress;

import com.mediatoolkit.pareco.model.ChunkInfo;
import com.mediatoolkit.pareco.model.DirectoryStructure;
import com.mediatoolkit.pareco.model.FileMetadata;
import com.mediatoolkit.pareco.model.FilePath;
import com.mediatoolkit.pareco.progress.StatsRecordingTransferProgressListener.FileTransferStats;
import static com.mediatoolkit.pareco.util.Util.divRound1d;
import static com.mediatoolkit.pareco.util.Util.durationPretty;
import static com.mediatoolkit.pareco.util.Util.fileSizePretty;
import static com.mediatoolkit.pareco.util.Util.round1d;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import static org.fusesource.jansi.Ansi.Color;
import static org.fusesource.jansi.Ansi.ansi;
import org.fusesource.jansi.AnsiConsole;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;


/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 30/10/2018
 */
public class LoggingTransferProgressListener implements TransferProgressListener {

	private static final Logger log = org.slf4j.LoggerFactory.getLogger("Transfer");

	private final Marker transferTaskMarker = MarkerFactory.getMarker("TRANSFER_TASK");
	private final Marker deleteMarker = MarkerFactory.getMarker("DELETE");
	private final Marker fileMarker = MarkerFactory.getMarker("FILE");
	private final Marker chunkMarker = MarkerFactory.getMarker("CHUNK");
	private final Marker speedMarker = MarkerFactory.getMarker("SPEED");

	private final StatsRecordingTransferProgressListener statsListener;
	private final Speedometer speedometer;
	private final LoggingFilter loggingFilter;

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

	private void logTransfer(String msg, Object... objects) {
		log.info(transferTaskMarker, ansi().fgBright(Color.DEFAULT).a(msg).reset().toString(), objects);
	}

	private void logDelete(String msg, Object... objects) {
		msg = highlightFile(msg);
		log.info(deleteMarker, ansi().fgBright(Color.YELLOW).a(msg).reset().toString(), objects);
	}

	private void logFile(String msg, Object... objects) {
		msg = highlightFile(msg);
		log.info(fileMarker, ansi().fgBright(Color.CYAN).a(msg).reset().toString(), objects);
	}

	private void logChunk(String msg, Object... objects) {
		msg = highlightFile(msg);
		log.info(chunkMarker, ansi().fg(Color.BLACK).a(msg).reset().toString(), objects);
	}

	private void logSpeed(String msg, Object... objects) {
		log.info(speedMarker, ansi().fgBright(Color.GREEN).a(msg).reset().toString(), objects);
	}

	private String highlightFile(String msg) {
		return msg.replace("'{}'", ansi().fgBright(Color.BLUE).a("'{}'").reset().toString());
	}

	@Builder
	private LoggingTransferProgressListener(
		@NonNull LoggingFilter loggingFilter,
		boolean forceColors,
		StatsRecordingTransferProgressListener statsListener,
		Speedometer speedometer
	) {
		this.statsListener = statsListener;
		this.speedometer = speedometer;
		this.loggingFilter = loggingFilter;
		if (forceColors) {
			System.setProperty("jansi.force", "true");
		}
		AnsiConsole.systemInstall();
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

	private Duration etaDuration(double speed) {
		if (speed <= 0) {
			return null;
		}
		long bytesLeft = statsListener.getTotalSize()
			- statsListener.getTransferredBytes()
			- statsListener.getSkippedBytes();
		double secondsLeft = bytesLeft / speed;
		return Duration.ofMillis((long) (1000 * secondsLeft));
	}

	@Override
	public void initializing(String transferMode, String sourceRootDir, String destinationRootDir) {
		logTransfer("Initializing {} transfer", transferMode);
		logTransfer("  - Source dir: '{}'", sourceRootDir);
		logTransfer("  - Destination dir: '{}'", destinationRootDir);
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
			logTransfer("  - Num directories: {}", statsListener.getNumDirectories());
			logTransfer("  - Num files: {}", statsListener.getNumFiles());
			logTransfer("  - Total size: {}", fileSizePretty(statsListener.getTotalSize()));
		}
		logTransfer("--------------------------------------------");
	}

	@Override
	public void deletedFiles(List<FilePath> filePaths) {
		if (!loggingFilter.isFiles()) {
			return;
		}
		filePaths.forEach(filePath -> logDelete("Deleted unexpected file: '{}'", filePath));
		logDelete("Deleted files: {}", filePaths.size());
	}

	@Override
	public void deletedDirectories(List<FilePath> filePaths) {
		if (!loggingFilter.isFiles()) {
			return;
		}
		filePaths.forEach(filePath -> logDelete("Deleted unexpected directory: '{}'", filePath));
		logDelete("Deleted directories: {}", filePaths.size());
	}

	@Override
	public void fileAnalyze(FilePath filePath) {
		if (!loggingFilter.isFiles()) {
			return;
		}
		long fileSize = fileMetadataMap.get(filePath).getFileSizeBytes();
		if (statsListener == null) {
			logFile("[{}] | Analyzing file: '{}'", fileSizePretty(fileSize), filePath);
		}
		logFile("Total: {}% | ({}) [{}] | Analyzing file: '{}'",
			totalProgressPercent(), fileRank(filePath), fileSizePretty(fileSize), filePath
		);
	}

	@Override
	public void fileSkipped(FilePath filePath) {
		if (!loggingFilter.isFiles()) {
			return;
		}
		long fileSize = fileMetadataMap.get(filePath).getFileSizeBytes();
		if (statsListener == null) {
			logFile("[{}] | Skipping file: '{}'", fileSizePretty(fileSize), filePath);
		}
		logFile("Total: {}% | ({}) [{}] | Skipping file: '{}'",
			totalProgressPercent(), fileRank(filePath), fileSizePretty(fileSize), filePath
		);
	}

	@Override
	public void fileStarted(FilePath filePath) {
		if (!loggingFilter.isFiles()) {
			return;
		}
		long fileSize = fileMetadataMap.get(filePath).getFileSizeBytes();
		if (statsListener == null) {
			logFile("[{}] | Start transfer file: '{}'", fileSizePretty(fileSize), filePath);
		}
		logFile("Total: {}% | ({}) [{}] | Start transfer file: '{}'",
			totalProgressPercent(), fileRank(filePath), fileSizePretty(fileSize), filePath
		);
	}

	@Override
	public void fileCompleted(FilePath filePath) {
		if (!loggingFilter.isFiles()) {
			return;
		}
		long fileSize = fileMetadataMap.get(filePath).getFileSizeBytes();
		if (statsListener == null) {
			logFile("[{}] | Completed file '{}'", fileSizePretty(fileSize), filePath);
			return;
		}
		FileTransferStats fileStats = statsListener.getFileStats(filePath);
		double effectiveSpeed = divRound1d(1000 * fileSize, fileStats.totalTime());
		logFile("Total: {}% | ({}) [{}] | Completed file in {}, speed {} '{}'",
			totalProgressPercent(), fileRank(filePath), fileSizePretty(fileSize),
			durationPretty(Duration.ofMillis(fileStats.totalTime())),
			fileSizePretty(effectiveSpeed) + "/s",
			filePath
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
			logChunk("[{}] | ({}) [{}] | Skipping chunk in file: '{}'",
				fileSizePretty(fileSize), chunkRank, fileSizePretty(chunkInfo.getSizeBytes()), filePath
			);
			return;
		}
		logChunk("Total: {}% | ({}) [{}] | ({}) [{}] | Skipping chunk in file: '{}'",
			totalProgressPercent(), fileRank(filePath), fileSizePretty(fileSize),
			chunkRank, fileSizePretty(chunkInfo.getSizeBytes()),
			filePath
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
				logSpeed("Transfer speed: {}", fileSizePretty(speed) + "/s");
			} else {
				Duration eta = etaDuration(speed);
				String etaPretty = eta == null ? "--:--" : durationPretty(eta);
				logSpeed("Total: {}% | Transfer speed: {} ETA: {}",
					totalProgressPercent(),
					fileSizePretty(speed) + "/s",
					etaPretty
				);
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
			logChunk("[{}] | ({}) [{}] | Completed chunk in file: '{}'",
				fileSizePretty(fileSize), chunkRank, fileSizePretty(chunkInfo.getSizeBytes()), filePath
			);
			return;
		}
		logChunk("Total: {}% | ({}) [{}] | ({}) [{}] | Completed chunk in file: '{}'",
			totalProgressPercent(), fileRank(filePath), fileSizePretty(fileSize),
			chunkRank, fileSizePretty(chunkInfo.getSizeBytes()),
			filePath
		);
	}

	@Override
	public void completed() {
		if (!loggingFilter.isStartEnd()) {
			return;
		}
		if (statsListener == null) {
			logTransfer("--------------------------------------------");
			logTransfer("Transfer task is completed");
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
		logTransfer("Transfer task is completed, stats:");
		logTransfer("  - Total duration: {}", totalDuration);
		logTransfer("  - Total size: {}", totalSizePretty);
		logTransfer("     - Skipped: {} ({}%)", skippedSize, skippedSizePercent);
		logTransfer("     - Transferred: {} ({}%)", transferredSize, transferredSizePercent);
		logTransfer("  - Num directories: {}", numDirectories);
		logTransfer("     - Deleted: {}", statsListener.getDeletedDirectories());
		logTransfer("  - Num files: {}", numFiles);
		logTransfer("     - Skipped: {} ({}%)", numSkippedFiles, numSkippedPercent);
		logTransfer("         - Num chunks: {}", numSkippedFilesChunks);
		logTransfer("     - Transferred: {} ({}%)", numTransferredFiles, numTransferredPercent);
		logTransfer("         - Num chunks: {}", numTransferredFilesChunks);
		logTransfer("             - Skipped: {} ({}%)", statsListener.getSkippedChunks(), skippedChunksPercent);
		logTransfer("             - Transferred: {} ({}%)", statsListener.getTransferredChunks(), transferredChunksPercent);
		logTransfer("     - Deleted: {}", statsListener.getDeletedFiles());
		logTransfer("  - Speed:");
		logTransfer("     - Average: {}", fileSizePretty(avgSpeed) + "/s");
		logTransfer("     - Effective: {}", fileSizePretty(effectiveAvgSpeed) + "/s");
		logTransfer("--------------------------------------------");
	}

	@Override
	public void aborted() {
		if (!loggingFilter.isStartEnd()) {
			return;
		}
		logTransfer("--------------------------------------------");
		if (statsListener == null) {
			logTransfer("Transfer task is aborted");
		} else {
			String totalDuration = durationPretty(Duration.ofMillis(statsListener.totalTime()));
			logTransfer("Transfer task is aborted, transfer {}% duration: {}", totalProgressPercent(), totalDuration);
		}
		logTransfer("--------------------------------------------");
	}
}
