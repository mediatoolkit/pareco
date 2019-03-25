package com.mediatoolkit.pareco.restclient;

import com.mediatoolkit.pareco.components.TransferNamesEncoding;
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
import lombok.NonNull;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 25/10/2018
 */
public class ListClient {

	private final RestTemplate restTemplate;
	private final String httpScheme;
	private final String host;
	private final int port;
	private final String authToken;
	private final TransferNamesEncoding encoding;

	@Builder
	public ListClient(
		@NonNull String httpScheme,
		@NonNull String host,
		@NonNull Integer port,
		@NonNull Integer connectTimeout,
		@NonNull Integer readTimeout,
		String authToken,
		@NonNull TransferNamesEncoding encoding
	) {
		this.httpScheme = httpScheme;
		this.host = host;
		this.port = port;
		this.authToken = authToken;
		restTemplate = new RestTemplateBuilder()
			.errorHandler(new TransferRestErrorHandler())
			.setConnectTimeout(connectTimeout)
			.setReadTimeout(readTimeout)
			.build();
		this.encoding = encoding;
	}

	private String encode(String val) {
		return encoding.encode(val);
	}

	public DirectoryStructure listDirectory(
		String serverRootDirectory, String include, String exclude
	) {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
			.scheme(httpScheme).host(host).port(port)
			.path("/list/dir")
			.queryParam("serverRootDirectory", encode(serverRootDirectory));
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
		return restTemplate.getForObject(uri, DirectoryStructure.class);
	}

}
