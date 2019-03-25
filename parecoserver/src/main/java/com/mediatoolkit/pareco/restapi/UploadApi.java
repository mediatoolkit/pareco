package com.mediatoolkit.pareco.restapi;

import com.mediatoolkit.pareco.components.TransferNamesEncoding;
import com.mediatoolkit.pareco.model.DigestType;
import com.mediatoolkit.pareco.model.DirectoryStructure;
import com.mediatoolkit.pareco.model.FileDigest;
import com.mediatoolkit.pareco.model.FilePath;
import com.mediatoolkit.pareco.session.SessionRepository;
import com.mediatoolkit.pareco.session.UploadSession;
import com.mediatoolkit.pareco.session.UploadSession.FileUploadSession;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 24/10/2018
 */
@RequestMapping("/upload")
@AllArgsConstructor(onConstructor = @__(@Autowired))
@RestController
public class UploadApi {

	private final SessionRepository sessionRepository;
	private final TransferNamesEncoding encoding;

	private String decode(String val) {
		return encoding.decode(val);
	}

	@PostMapping("/init")
	public String initializeUpload(
		@RequestParam("serverRootDirectory") String serverRootDirectory,
		@RequestParam("chunkSizeBytes") Long chunkSizeBytes,
		@RequestParam(name = "authToken", required = false) String authToken,
		@RequestParam(name = "include", required = false) String include,
		@RequestParam(name = "exclude", required = false) String exclude,
		@RequestBody DirectoryStructure directoryStructure
	) {
		return sessionRepository.initNewUploadSession(
			decode(serverRootDirectory), directoryStructure, chunkSizeBytes, authToken, decode(include), decode(exclude)
		);
	}

	@GetMapping("/structure")
	public DirectoryStructure getDirectoryStructure(
		@RequestParam("uploadSession") String transferSession
	) throws IOException {
		UploadSession uploadSession = sessionRepository.getUploadSession(transferSession);
		return uploadSession.getCurrentDirectoryStructure();
	}

	@PostMapping("/create_directories")
	public void createRequiredDirectories(
		@RequestParam("uploadSession") String transferSession
	) throws IOException {
		UploadSession uploadSession = sessionRepository.getUploadSession(transferSession);
		uploadSession.createRequiredDirectories();
	}

	@PostMapping("/file/delete")
	public void deleteMany(
		@RequestParam("uploadSession") String transferSession,
		@RequestBody List<FilePath> filePaths
	) throws IOException {
		UploadSession uploadSession = sessionRepository.getUploadSession(transferSession);
		uploadSession.deleteFiles(filePaths);
	}

	@GetMapping("/file/digest")
	public FileDigest getFileDigest(
		@RequestParam("uploadSession") String transferSession,
		@RequestParam("relativeDirectory") String relativeDirectory,
		@RequestParam("fileName") String fileName,
		@RequestParam("digestType") DigestType digestType
	) throws IOException {
		UploadSession uploadSession = sessionRepository.getUploadSession(transferSession);
		return uploadSession.getFileDigest(decode(relativeDirectory), decode(fileName), digestType);
	}

	@PutMapping("/file/skip")
	public void skipFileUpload(
		@RequestParam("uploadSession") String transferSession,
		@RequestParam("relativeDirectory") String relativeDirectory,
		@RequestParam("fileName") String fileName
	) {
		UploadSession uploadSession = sessionRepository.getUploadSession(transferSession);
		uploadSession.skipFileUpload(decode(relativeDirectory), decode(fileName));
	}

	@PostMapping("/file/init")
	public String initializeFileUpload(
		@RequestParam("uploadSession") String transferSession,
		@RequestParam("relativeDirectory") String relativeDirectory,
		@RequestParam("fileName") String fileName
	) throws IOException {
		UploadSession uploadSession = sessionRepository.getUploadSession(transferSession);
		return uploadSession.initFileUploadSession(decode(relativeDirectory), decode(fileName));
	}

	@PutMapping("/file/chunk")
	public void uploadChunk(
		@RequestParam("fileUploadSession") String fileTransferSession,
		@RequestParam("offsetBytes") Long offsetBytes,
		@RequestParam("sizeBytes") Long sizeBytes,
		InputStream inputStream
	) throws IOException {
		FileUploadSession uploadSession = sessionRepository.getFileUploadSession(fileTransferSession);
		uploadSession.uploadChunk(offsetBytes, sizeBytes, inputStream);
	}

	@PutMapping("/file/commit")
	public void commitFileUpload(
		@RequestParam("fileUploadSession") String fileTransferSession
	) {
		FileUploadSession uploadSession = sessionRepository.getFileUploadSession(fileTransferSession);
		uploadSession.commit();
	}

	@DeleteMapping("/file/delete")
	public void deleteFile(
		@RequestParam("fileUploadSession") String fileTransferSession
	) {
		FileUploadSession uploadSession = sessionRepository.getFileUploadSession(fileTransferSession);
		uploadSession.delete();
	}

	@PutMapping("/commit")
	public void commitUpload(
		@RequestParam("uploadSession") String transferSession
	) {
		sessionRepository.completeUploadSession(transferSession);
	}

	@PutMapping("/abort")
	public void abortUpload(
		@RequestParam("uploadSession") String transferSession
	) {
		sessionRepository.abortUploadSession(transferSession);
	}

}
