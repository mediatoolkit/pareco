package com.mediatoolkit.pareco.session;

import com.mediatoolkit.pareco.auth.Authenticator;
import com.mediatoolkit.pareco.components.PathOverlapAnalyzer;
import com.mediatoolkit.pareco.exceptions.SessionNotExistsException;
import com.mediatoolkit.pareco.exceptions.SessionRootDirectoryClashException;
import com.mediatoolkit.pareco.model.DirectoryStructure;
import com.mediatoolkit.pareco.session.DownloadSession.FileDownloadSession;
import com.mediatoolkit.pareco.session.UploadSession.FileUploadSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.EntryStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 05/11/2018
 */
@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SessionRepository {

	private final Authenticator authenticator;
	private final SessionFactory sessionFactory;
	private final PathOverlapAnalyzer pathOverlapAnalyzer;
	@Value("${session.expire.max_inactive}")
	private long maxSessionInactivity;

	private final Map<String, UploadSession> uploadSessions = new HashMap<>();
	private final Map<String, DownloadSession> downloadSessions = new HashMap<>();

	@Synchronized
	public String initNewDownloadSession(
		String rootDirectory, long chunkSizeBytes, String authToken, String include, String exclude
	) throws IOException {
		authenticator.authenticate(authToken);
		checkIfDirOverlapsWithExistingSession(rootDirectory);
		log.info("Initializing new download session, dir: '{}'", rootDirectory);
		String id = UUID.randomUUID().toString();
		DownloadSession session = sessionFactory.newDownloadSession(
			id, rootDirectory, chunkSizeBytes, include, exclude
		);
		downloadSessions.put(id, session);
		return id;
	}

	@Synchronized
	public void completeDownloadSession(String downloadSessionId) {
		DownloadSession downloadSession = downloadSessions.get(downloadSessionId);
		if (downloadSession != null) {
			downloadSession.commit();
			downloadSessions.remove(downloadSessionId);
			log.info("Download session completed, dir: '{}'", downloadSession.getRootDirectory());
		}
	}

	@Synchronized
	public void abortDownloadSession(String downloadSessionId) {
		DownloadSession downloadSession = downloadSessions.get(downloadSessionId);
		if (downloadSession != null) {
			downloadSession.abort();
			downloadSessions.remove(downloadSessionId);
			log.info("Download session aborted, dir: '{}'", downloadSession.getRootDirectory());
		}
	}

	@Synchronized
	public DownloadSession getDownloadSession(String downloadSessionId) {
		DownloadSession session = downloadSessions.get(downloadSessionId);
		if (session == null) {
			throw new SessionNotExistsException("Download session id: " + downloadSessionId);
		}
		return session;
	}

	@Synchronized
	public FileDownloadSession getFileDownloadSession(String fileDownloadSessionId) {
		String uploadSessionId = fileDownloadSessionId.substring(0, fileDownloadSessionId.indexOf('_'));
		DownloadSession downloadSession = getDownloadSession(uploadSessionId);
		return downloadSession.getFileDownloadSession(fileDownloadSessionId);
	}

	public String initNewUploadSession(
		String rootDirectory, DirectoryStructure directoryStructure,
		long chunkSizeBytes, String authToken, String include, String exclude
	) {
		authenticator.authenticate(authToken);
		checkIfDirOverlapsWithExistingSession(rootDirectory);
		log.info("Initializing new upload session, dir: '{}'", rootDirectory);
		String id = UUID.randomUUID().toString();
		UploadSession session = sessionFactory.newUploadSession(
			id, rootDirectory, directoryStructure, chunkSizeBytes, include, exclude
		);
		uploadSessions.put(id, session);
		return id;
	}

	@Synchronized
	public void completeUploadSession(String uploadSessionId) {
		UploadSession uploadSession = uploadSessions.get(uploadSessionId);
		if (uploadSession != null) {
			uploadSession.commit();
			uploadSessions.remove(uploadSessionId);
			log.info("Upload session completed, dir: '{}'", uploadSession.getRootDirectory());
		}
	}

	@Synchronized
	public void abortUploadSession(String uploadSessionId) {
		UploadSession uploadSession = uploadSessions.get(uploadSessionId);
		if (uploadSession != null) {
			uploadSession.abort();
			uploadSessions.remove(uploadSessionId);
			log.info("Upload session aborted, dir: '{}'", uploadSession.getRootDirectory());
		}
	}

	@Synchronized
	public UploadSession getUploadSession(String uploadSessionId) {
		UploadSession uploadSession = uploadSessions.get(uploadSessionId);
		if (uploadSession == null) {
			throw new SessionNotExistsException("Upload session id: " + uploadSessionId);
		}
		return uploadSession;
	}

	@Synchronized
	public FileUploadSession getFileUploadSession(String fileUploadSessionId) {
		String uploadSessionId = fileUploadSessionId.substring(0, fileUploadSessionId.indexOf('_'));
		UploadSession uploadSession = getUploadSession(uploadSessionId);
		return uploadSession.getFileUploadSession(fileUploadSessionId);
	}

	private void checkIfDirOverlapsWithExistingSession(String rootDirectory) {
		uploadSessions.values().forEach(uploadSession -> {
			String sessionRootDirectory = uploadSession.getRootDirectory();
			boolean pathsDoOverlap = pathOverlapAnalyzer.pathsDoOverlap(rootDirectory, sessionRootDirectory);
			if (pathsDoOverlap) {
				throw new SessionRootDirectoryClashException(String.format(
					"Given rootDirectory '%s' overlaps with existing upload session's root directory '%s'",
					rootDirectory, sessionRootDirectory
				));
			}
		});
		downloadSessions.values().forEach(downloadSession -> {
			String sessionRootDirectory = downloadSession.getRootDirectory();
			boolean pathsDoOverlap = pathOverlapAnalyzer.pathsDoOverlap(rootDirectory, sessionRootDirectory);
			if (pathsDoOverlap) {
				throw new SessionRootDirectoryClashException(String.format(
					"Given rootDirectory '%s' overlaps with existing download session's root directory '%s'",
					rootDirectory, sessionRootDirectory
				));
			}
		});
	}

	@Synchronized
	public void expireInactiveSessions() {
		log.trace("Checking if need to expire sessions");
		long now = System.currentTimeMillis();
		List<String> inactiveUploadSessionIds = EntryStream.of(uploadSessions)
			.filterValues(uploadSession -> now - uploadSession.getLastActivityTime() > maxSessionInactivity)
			.keys()
			.toList();
		List<String> inactiveDownloadSessionIds = EntryStream.of(downloadSessions)
			.filterValues(downloadSession -> now - downloadSession.getLastActivityTime() > maxSessionInactivity)
			.keys()
			.toList();
		inactiveUploadSessionIds.stream()
			.peek(session -> log.debug("Going to expire/abort inactive upload session"))
			.forEach(this::abortUploadSession);
		inactiveDownloadSessionIds.stream()
			.peek(session -> log.debug("Going to expire/abort inactive download session"))
			.forEach(this::abortDownloadSession);
	}

}
