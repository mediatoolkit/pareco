package com.mediatoolkit.pareco.restapi;

import com.mediatoolkit.pareco.components.TransferNamesEncoding;
import com.mediatoolkit.pareco.model.DigestType;
import com.mediatoolkit.pareco.model.DirectoryStructure;
import com.mediatoolkit.pareco.model.FileDigest;
import com.mediatoolkit.pareco.session.DownloadSession;
import com.mediatoolkit.pareco.session.DownloadSession.FileDownloadSession;
import com.mediatoolkit.pareco.session.SessionRepository;
import java.io.IOException;
import java.io.OutputStream;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 24/10/2018
 */
@RequestMapping("/download")
@AllArgsConstructor(onConstructor = @__(@Autowired))
@RestController
public class DownloadApi {

	private final SessionRepository sessionRepository;
	private final TransferNamesEncoding encoding;

	private String decode(String val) {
		if (val == null) {
			return null;
		}
		return encoding.decode(val);
	}

	@PostMapping("/init")
	public String initializeDownload(
		@RequestParam("serverRootDirectory") String serverRootDirectory,
		@RequestParam("chunkSizeBytes") Long chunkSizeBytes,
		@RequestParam(name = "authToken", required = false) String authToken,
		@RequestParam(name = "include", required = false) String include,
		@RequestParam(name = "exclude", required = false) String exclude
	) throws IOException {
		return sessionRepository.initNewDownloadSession(
			decode(serverRootDirectory), chunkSizeBytes, authToken, decode(include), decode(exclude)
		);
	}

	@GetMapping("/structure")
	public DirectoryStructure getDirectoryStructure(
		@RequestParam("downloadSession") String transferSession
	) {
		DownloadSession downloadSession = sessionRepository.getDownloadSession(transferSession);
		return downloadSession.getDirectoryStructure();
	}

	@GetMapping("/file/digest")
	public FileDigest getFileDigest(
		@RequestParam("downloadSession") String transferSession,
		@RequestParam("relativeDirectory") String relativeDirectory,
		@RequestParam("fileName") String fileName,
		@RequestParam("digestType") DigestType digestType
	) throws IOException {
		DownloadSession downloadSession = sessionRepository.getDownloadSession(transferSession);
		return downloadSession.getFileDigest(decode(relativeDirectory), decode(fileName), digestType);
	}

	@PutMapping("/file/skip")
	public void skipFileDownload(
		@RequestParam("downloadSession") String transferSession,
		@RequestParam("relativeDirectory") String relativeDirectory,
		@RequestParam("fileName") String fileName
	) {
		DownloadSession downloadSession = sessionRepository.getDownloadSession(transferSession);
		downloadSession.skipFileDownload(decode(relativeDirectory), decode(fileName));
	}

	@PostMapping("/file/init")
	public String initializeFileDownload(
		@RequestParam("downloadSession") String transferSession,
		@RequestParam("relativeDirectory") String relativeDirectory,
		@RequestParam("fileName") String fileName
	) throws IOException {
		DownloadSession downloadSession = sessionRepository.getDownloadSession(transferSession);
		return downloadSession.initFileDownloadSession(decode(relativeDirectory), decode(fileName));
	}

	@GetMapping("/file/chunk")
	public void downloadChunk(
		@RequestParam("fileDownloadSession") String fileTransferSession,
		@RequestParam("offsetBytes") Long offsetBytes,
		@RequestParam("sizeBytes") Long sizeBytes,
		OutputStream outputStream
	) throws IOException {
		FileDownloadSession fileDownloadSession = sessionRepository.getFileDownloadSession(fileTransferSession);
		fileDownloadSession.downloadChunk(offsetBytes, sizeBytes, outputStream);
	}

	@PutMapping("/file/commit")
	public void commitFileDownload(
		@RequestParam("fileDownloadSession") String fileTransferSession
	) {
		FileDownloadSession fileDownloadSession = sessionRepository.getFileDownloadSession(fileTransferSession);
		fileDownloadSession.commit();
	}

	@PutMapping("/commit")
	public void commitDownload(
		@RequestParam("downloadSession") String transferSession
	) {
		sessionRepository.completeDownloadSession(transferSession);
	}

	@PutMapping("/abort")
	public void abortDownload(
		@RequestParam("downloadSession") String transferSession
	) {
		sessionRepository.abortDownloadSession(transferSession);
	}


}
