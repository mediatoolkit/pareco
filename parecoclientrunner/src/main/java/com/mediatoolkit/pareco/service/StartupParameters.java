package com.mediatoolkit.pareco.service;

import com.mediatoolkit.pareco.exceptions.ParecoException;
import com.mediatoolkit.pareco.model.ErrorBody.Type;
import com.mediatoolkit.pareco.transfer.model.ServerInfo;
import com.mediatoolkit.pareco.transfer.model.TransferJob;
import com.mediatoolkit.pareco.transfer.model.TransferTask;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 2019-03-25
 */
@Service
@Getter
public class StartupParameters {

	private final ServerInfo server;
	private final String localDir;
	private final String remoteDir;

	public StartupParameters(
		@Value("${fixed-parameter.server.scheme:#{null}}") String serverScheme,
		@Value("${fixed-parameter.server.host:#{null}}") String serverHost,
		@Value("${fixed-parameter.server.port:#{null}}") Integer serverPort,
		@Value("${fixed-parameter.local-dir:#{null}}") String localDir,
		@Value("${fixed-parameter.remote-dir:#{null}}") String remoteDir
	) {
		if (serverScheme != null && serverHost != null && serverPort != null) {
			server = new ServerInfo(serverScheme, serverHost, serverPort);
		} else {
			server = null;
		}
		this.localDir = localDir;
		this.remoteDir = remoteDir;
	}

	public boolean isServerSet() {
		return server != null;
	}

	public boolean isLocalDirSet() {
		return localDir != null;
	}

	public boolean isRemoteDirSet() {
		return remoteDir != null;
	}

	public void validate(TransferJob transferJob) {
		TransferTask transferTask = transferJob.getTransferTask();
		if (server != null && !server.equals(transferTask.getServerInfo())) {
			throw new StartupParameterConflictException(String.format(
				"Request's server (%s) is not equal to predefined server (%s)",
				transferTask.getServerInfo(), server
			));
		}
		if (localDir != null && !localDir.equals(transferTask.getLocalRootDirectory())) {
			throw new StartupParameterConflictException(String.format(
				"Request's localDir (%s) is not equal to predefined localdir (%s)",
				transferTask.getLocalRootDirectory(), localDir
			));
		}
		if (remoteDir != null && !remoteDir.equals(transferTask.getRemoteRootDirectory())) {
			throw new StartupParameterConflictException(String.format(
				"Request's remoteDir (%s) is not equal to predefined remotedir (%s)",
				transferTask.getRemoteRootDirectory(), remoteDir
			));
		}
	}

	public static class StartupParameterConflictException extends ParecoException {

		public StartupParameterConflictException(String message) {
			super(message);
		}

		@Override
		public Type type() {
			return Type.ILLEGAL_STATE;
		}
	}
}
