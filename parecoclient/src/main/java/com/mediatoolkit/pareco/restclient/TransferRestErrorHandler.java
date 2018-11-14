package com.mediatoolkit.pareco.restclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediatoolkit.pareco.model.ErrorBody;
import com.mediatoolkit.pareco.restclient.TransferClientException.ServerSideTransferClientException;
import com.mediatoolkit.pareco.restclient.TransferClientException.UnknownErrorTransferClientException;
import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 30/10/2018
 */
public class TransferRestErrorHandler implements ResponseErrorHandler {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		return response.getStatusCode() != HttpStatus.OK;
	}

	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		byte[] body = IOUtils.toByteArray(response.getBody());
		try {
			ErrorBody errorBody = objectMapper.readValue(body, ErrorBody.class);
			throw new ServerSideTransferClientException(errorBody);
		} catch (IOException ignore) {
			throw new UnknownErrorTransferClientException(
				"unknown error body: " + new String(body, Charset.forName("utf-8"))
			);
		}
	}
}
