package com.mediatoolkit.pareco.restclient;

import com.mediatoolkit.pareco.model.ChunkInfo;
import com.mediatoolkit.pareco.model.DigestType;
import com.mediatoolkit.pareco.model.DirectoryStructure;
import com.mediatoolkit.pareco.model.FileDigest;
import com.mediatoolkit.pareco.model.FilePath;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 25/10/2018
 */
public class DownloadClient {

	private final RestTemplate restTemplate;
	private final String httpScheme;
	private final String host;
	private final int port;
	private final String authToken;

	@Builder
	public DownloadClient(
		String httpScheme, String host, int port, int timeout, String authToken
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
	}

	public DownloadSessionClient initializeDownload(
		String serverRootDirectory, Long chunkSizeBytes, String include, String exclude
	) {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
			.scheme(httpScheme).host(host).port(port)
			.path("/download/init")
			.queryParam("serverRootDirectory", serverRootDirectory)
			.queryParam("chunkSizeBytes", chunkSizeBytes);
		if (authToken != null) {
			builder.queryParam("authToken", authToken);
		}
		if (include != null) {
			builder.queryParam("include", include);
		}
		if (exclude != null) {
			builder.queryParam("exclude", exclude);
		}
		URI uri = builder.build().toUri();
		String downloadSession = restTemplate.postForObject(uri, null, String.class);
		return new DownloadSessionClient(downloadSession);
	}

	@AllArgsConstructor
	public class DownloadSessionClient {

		private final String downloadSession;

		public DirectoryStructure getDirectoryStructure() {
			URI uri = UriComponentsBuilder.newInstance()
				.scheme(httpScheme).host(host).port(port)
				.path("/download/structure")
				.queryParam("downloadSession", downloadSession)
				.build().toUri();
			return restTemplate.getForObject(uri, DirectoryStructure.class);
		}

		public FileDigest getFileDigest(FilePath filePath, DigestType digestType) {
			URI uri = UriComponentsBuilder.newInstance()
				.scheme(httpScheme).host(host).port(port)
				.path("/download/file/digest")
				.queryParam("downloadSession", downloadSession)
				.queryParam("relativeDirectory", filePath.getRelativeDirectory())
				.queryParam("fileName", filePath.getFileName())
				.queryParam("digestType", digestType)
				.build().toUri();
			return restTemplate.getForObject(uri, FileDigest.class);
		}

		public void skipFileDownload(FilePath filePath) {
			URI uri = UriComponentsBuilder.newInstance()
				.scheme(httpScheme).host(host).port(port)
				.path("/download/file/skip")
				.queryParam("downloadSession", downloadSession)
				.queryParam("relativeDirectory", filePath.getRelativeDirectory())
				.queryParam("fileName", filePath.getFileName())
				.build().toUri();
			restTemplate.put(uri, null);
		}

		public FileDownloadSessionClient initializeFileDownload(FilePath filePath) {
			URI uri = UriComponentsBuilder.newInstance()
				.scheme(httpScheme).host(host).port(port)
				.path("/download/file/init")
				.queryParam("downloadSession", downloadSession)
				.queryParam("relativeDirectory", filePath.getRelativeDirectory())
				.queryParam("fileName", filePath.getFileName())
				.build().toUri();
			String fileDownloadSession = restTemplate.postForObject(uri, null, String.class);
			return new FileDownloadSessionClient(fileDownloadSession);
		}

		public void commitDownload() {
			URI uri = UriComponentsBuilder.newInstance()
				.scheme(httpScheme).host(host).port(port)
				.path("/download/commit")
				.queryParam("downloadSession", downloadSession)
				.build().toUri();
			restTemplate.put(uri, null);
		}

		public void abortDownload() {
			URI uri = UriComponentsBuilder.newInstance()
				.scheme(httpScheme).host(host).port(port)
				.path("/download/abort")
				.queryParam("downloadSession", downloadSession)
				.build().toUri();
			restTemplate.put(uri, null);
		}

	}

	@AllArgsConstructor
	public class FileDownloadSessionClient {

		private final String fileDownloadSession;

		public void downloadChunk(ChunkInfo chunkInfo, InputStreamHandler inputStreamHandler) {
			URI uri = UriComponentsBuilder.newInstance()
				.scheme(httpScheme).host(host).port(port)
				.path("/download/file/chunk")
				.queryParam("fileDownloadSession", fileDownloadSession)
				.queryParam("offsetBytes", chunkInfo.getOffsetBytes())
				.queryParam("sizeBytes", chunkInfo.getSizeBytes())
				.build().toUri();
			ResponseExtractor<Void> responseExtractor = response -> {
				inputStreamHandler.handleInputStream(response.getBody());
				return null;
			};
			restTemplate.execute(uri, HttpMethod.GET, null, responseExtractor);
		}

		public void commitFileDownload() {
			URI uri = UriComponentsBuilder.newInstance()
				.scheme(httpScheme).host(host).port(port)
				.path("/download/file/commit")
				.queryParam("fileDownloadSession", fileDownloadSession)
				.build().toUri();
			restTemplate.put(uri, null);
		}

	}

	public interface InputStreamHandler {

		void handleInputStream(InputStream inputStream) throws IOException;

	}

	public static void main(String[] args) {
		DownloadClient downloadClient = DownloadClient.builder().build();
		DownloadSessionClient sessionClient = downloadClient.new DownloadSessionClient("");
		sessionClient.initializeFileDownload(FilePath.of("film[2018].avi"));
	}

}
