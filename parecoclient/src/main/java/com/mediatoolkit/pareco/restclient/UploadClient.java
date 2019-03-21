package com.mediatoolkit.pareco.restclient;

import com.mediatoolkit.pareco.components.TransferNamesEncoding;
import com.mediatoolkit.pareco.model.ChunkInfo;
import com.mediatoolkit.pareco.model.DigestType;
import com.mediatoolkit.pareco.model.DirectoryStructure;
import com.mediatoolkit.pareco.model.FileDigest;
import com.mediatoolkit.pareco.model.FilePath;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 25/10/2018
 */
public class UploadClient {

	private final RestTemplate restTemplate;
	private final TransferNamesEncoding encoding;
	private final String httpScheme;
	private final String host;
	private final int port;
	private final String authToken;

	@Builder
	public UploadClient(
		@NonNull String httpScheme,
		@NonNull String host,
		@NonNull Integer port,
		@NonNull Integer timeout,
		String authToken,
		@NonNull TransferNamesEncoding encoding
	) {
		this.httpScheme = httpScheme;
		this.host = host;
		this.port = port;
		this.authToken = authToken;
		restTemplate = new RestTemplateBuilder()
			.errorHandler(new TransferRestErrorHandler())
			.setConnectTimeout(timeout)
			.setReadTimeout(timeout)
			.build();
		this.encoding = encoding;
	}

	private String encode(String val) {
		return encoding.encode(val);
	}

	public UploadSessionClient initializeUpload(
		String serverRootDirectory, Long chunkSizeBytes, String include, String exclude, DirectoryStructure directoryStructure
	) {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
			.scheme(httpScheme).host(host).port(port)
			.path("/upload/init")
			.queryParam("serverRootDirectory", encode(serverRootDirectory))
			.queryParam("chunkSizeBytes", chunkSizeBytes);
		if (authToken != null) {
			builder.queryParam("authToken", authToken);
		}
		if (include != null) {
			builder.queryParam("include", encode(include));
		}
		if (exclude != null) {
			builder.queryParam("exclude", encode(exclude));
		}
		URI uri = builder.build().toUri();
		String uploadSession = restTemplate.postForObject(uri, directoryStructure, String.class);
		return new UploadSessionClient(uploadSession);
	}

	@AllArgsConstructor
	public class UploadSessionClient {

		private final String uploadSession;

		public DirectoryStructure getDirectoryStructure() {
			URI uri = UriComponentsBuilder.newInstance()
				.scheme(httpScheme).host(host).port(port)
				.path("/upload/structure")
				.queryParam("uploadSession", uploadSession)
				.build().toUri();
			return restTemplate.getForObject(uri, DirectoryStructure.class);
		}

		public void createDirectories() {
			URI uri = UriComponentsBuilder.newInstance()
				.scheme(httpScheme).host(host).port(port)
				.path("/upload/create_directories")
				.queryParam("uploadSession", uploadSession)
				.build().toUri();
			restTemplate.postForObject(uri, null, Void.class);
		}

		public void deleteFiles(List<FilePath> filePaths) {
			URI uri = UriComponentsBuilder.newInstance()
				.scheme(httpScheme).host(host).port(port)
				.path("/upload/file/delete")
				.queryParam("uploadSession", uploadSession)
				.build().toUri();
			restTemplate.postForObject(uri, filePaths, Void.class);
		}

		public FileDigest getFileDigest(FilePath filePath, DigestType digestType) {
			URI uri = UriComponentsBuilder.newInstance()
				.scheme(httpScheme).host(host).port(port)
				.path("/upload/file/digest")
				.queryParam("uploadSession", uploadSession)
				.queryParam("relativeDirectory", encode(filePath.getRelativeDirectory()))
				.queryParam("fileName", encode(filePath.getFileName()))
				.queryParam("digestType", digestType)
				.build().toUri();
			return restTemplate.getForObject(uri, FileDigest.class);
		}

		public void skipFileUpload(FilePath filePath) {
			URI uri = UriComponentsBuilder.newInstance()
				.scheme(httpScheme).host(host).port(port)
				.path("/upload/file/skip")
				.queryParam("uploadSession", uploadSession)
				.queryParam("relativeDirectory", encode(filePath.getRelativeDirectory()))
				.queryParam("fileName", encode(filePath.getFileName()))
				.build().toUri();
			restTemplate.put(uri, null);
		}

		public FileUploadSessionClient initializeFileUpload(FilePath filePath) {
			URI uri = UriComponentsBuilder.newInstance()
				.scheme(httpScheme).host(host).port(port)
				.path("/upload/file/init")
				.queryParam("uploadSession", uploadSession)
				.queryParam("relativeDirectory", encode(filePath.getRelativeDirectory()))
				.queryParam("fileName", encode(filePath.getFileName()))
				.build().toUri();
			String fileUploadSession = restTemplate.postForObject(uri, null, String.class);
			return new FileUploadSessionClient(fileUploadSession);
		}

		public void commitUpload() {
			URI uri = UriComponentsBuilder.newInstance()
				.scheme(httpScheme).host(host).port(port)
				.path("/upload/commit")
				.queryParam("uploadSession", uploadSession)
				.build().toUri();
			restTemplate.put(uri, null);
		}

		public void abortUpload() {
			URI uri = UriComponentsBuilder.newInstance()
				.scheme(httpScheme).host(host).port(port)
				.path("/upload/abort")
				.queryParam("uploadSession", uploadSession)
				.build().toUri();
			restTemplate.put(uri, null);
		}

	}

	@AllArgsConstructor
	public class FileUploadSessionClient {

		private final String fileUploadSession;

		public void uploadChunk(ChunkInfo chunkInfo, InputStream inputStream) {
			URI uri = UriComponentsBuilder.newInstance()
				.scheme(httpScheme).host(host).port(port)
				.path("/upload/file/chunk")
				.queryParam("fileUploadSession", fileUploadSession)
				.queryParam("offsetBytes", chunkInfo.getOffsetBytes())
				.queryParam("sizeBytes", chunkInfo.getSizeBytes())
				.build().toUri();
			RequestCallback requestCallback = request -> {
				OutputStream outputStream = request.getBody();
				IOUtils.copy(inputStream, outputStream);
			};
			restTemplate.execute(uri, HttpMethod.PUT, requestCallback, null);
		}

		public void commitFileUpload() {
			URI uri = UriComponentsBuilder.newInstance()
				.scheme(httpScheme).host(host).port(port)
				.path("/upload/file/commit")
				.queryParam("fileUploadSession", fileUploadSession)
				.build().toUri();
			restTemplate.put(uri, null);
		}

		public void deleteFile() {
			URI uri = UriComponentsBuilder.newInstance()
				.scheme(httpScheme).host(host).port(port)
				.path("/upload/file/delete")
				.queryParam("fileUploadSession", fileUploadSession)
				.build().toUri();
			restTemplate.delete(uri);
		}

	}

}
