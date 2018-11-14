package com.mediatoolkit.pareco.commandline;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.mediatoolkit.pareco.model.DigestType;
import com.mediatoolkit.pareco.progress.TransferLoggingLevel;
import com.mediatoolkit.pareco.transfer.model.ServerInfo;
import com.mediatoolkit.pareco.transfer.model.TransferMode;
import com.mediatoolkit.pareco.transfer.model.TransferOptions;
import com.mediatoolkit.pareco.transfer.model.TransferOptions.FileIntegrityOptions;
import com.mediatoolkit.pareco.transfer.model.TransferTask;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 02/11/2018
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Parameters(separators = " =")
@Slf4j
public class CommandLineOptions {

	@Parameter(names = {"-m", "--mode"}, required = true, description = "Transfer mode to execute", order = 1)
	private TransferMode mode;

	@Parameter(
		names = {"-s", "--server"},
		required = true,
		description = "Server url, example: http://my.server.com:8080",
		converter = ServerInfoConverter.class,
		order = 2
	)
	private ServerInfo server;

	@Parameter(names = {"-l", "--localDir"}, required = true, description = "Local directory to upload from or to download into", order = 3)
	private String localDir;

	@Parameter(names = {"-r", "--remoteDir"}, required = true, description = "Remote directory to download from or to upload into", order = 3)
	private String remoteDir;

	@Parameter(names = {"-i", "--include"}, description = "File glob pattern for inclusion filter, example: subDir/*/archive.zip", order = 4)
	private String include;

	@Parameter(names = {"-e", "--exclude"}, description = "File glob pattern for exclusion filter, example: **/*.log", order = 4)
	private String exclude;

	@Parameter(names = {"-n", "--numConnections"}, description = "Number of parallel transport connections", order = 5)
	private int numTransferConnections = 10;

	@Parameter(names = {"-t", "--timeout"}, description = "Connect and read timeout on connections in milliseconds", order = 5)
	private int timeout = 120_000;

	@Parameter(
		names = {"-del", "--deleteUnexpected"},
		description = "If set, all unexpected files and directories on destination will be deleted"
	)
	private boolean deleteUnexpected = false;

	@Parameter(
		names = {"-c", "--chunk"},
		description = "Size of chunk to split file, examples: 200, 30K, 2M",
		converter = ChunkSizeConverter.class
	)
	private long chunkSizeBytes = 1 << 20;    //1M

	@Parameter(
		names = "--skipDigest",
		description = "If set, then calculating and checking file checksum will be ignored, file integrity will only be checked by file size and last modified time"
	)
	private boolean skipDigestCheck = false;

	@Parameter(names = "--hash", description = "Which hash function to use for file digest checksum")
	private DigestType digestType = DigestType.CRC_32;

	@Parameter(names = {"-log", "--logLevel"}, description = "Choose which logging level to use")
	private TransferLoggingLevel loggingLevel = TransferLoggingLevel.FILES;

	@Parameter(names = {"--noStats"}, description = "Choose to disable tracking and logging statistics of transport")
	private boolean noTransferStats = false;

	@Parameter(names = {"-a", "--authToken"}, description = "Authentication token to use if authentication is enabled on server")
	private String authToken;

	@Parameter(names = {"-h", "--help"}, help = true, description = "Print this help with parameters", order = 1000)
	private boolean help;

	@Parameter(names = {"-v", "--version"}, help = true, description = "Show current build version", order = 1000)
	private boolean version;

	public void validate() throws ParameterException {
		if (numTransferConnections <= 0) {
			throw new ParameterException("Number of transfer connections must be positive integer, got: " + numTransferConnections);
		}
		if (authToken != null && authToken.isEmpty()) {
			throw new ParameterException("Auth token must not be empty string");
		}
		if (chunkSizeBytes <= 1) {
			throw new ParameterException("Chunk size must be >= 1, got: " + chunkSizeBytes);
		} else if (chunkSizeBytes < 1024) {
			log.warn("Chunk size is very low ({} bytes) performance might be poor", chunkSizeBytes);
		}
		if (timeout < 0) {
			throw new ParameterException("Timeout must not be negative, got: " + timeout);
		}
	}

	public TransferTask toTransferTask() {
		FileIntegrityOptions fileIntegrityOptions = skipDigestCheck
			? FileIntegrityOptions.onlyMetadata()
			: FileIntegrityOptions.metadataAndDigest(digestType);
		return TransferTask.builder()
			.localRootDirectory(localDir)
			.remoteRootDirectory(remoteDir)
			.include(include)
			.exclude(exclude)
			.serverInfo(server)
			.authToken(authToken)
			.options(TransferOptions.builder()
				.chunkSizeBytes(chunkSizeBytes)
				.deleteUnexpected(deleteUnexpected)
				.numTransferConnections(numTransferConnections)
				.timeout(timeout)
				.fileIntegrityOptions(fileIntegrityOptions)
				.build()
			)
			.build();
	}

}
