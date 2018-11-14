package com.mediatoolkit.pareco.commandline;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;
import com.mediatoolkit.pareco.transfer.model.ServerInfo;
import com.mediatoolkit.pareco.util.Util;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 02/11/2018
 */
public class ServerInfoConverter extends BaseConverter<ServerInfo> {

	public ServerInfoConverter(String optionName) {
		super(optionName);
	}

	@Override
	public ServerInfo convert(String value) {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(value).build();
		String scheme = Util.thisOrDefault(uriComponents.getScheme(), "http");
		String host = uriComponents.getHost();
		if (host == null) {
			throw new ParameterException(getErrorString(value, "ServerInfo's host"));
		}
		int port = uriComponents.getPort();
		if (port == -1) {
			switch (scheme) {
				case "http":
					port = 80;
					break;
				case "https":
					port = 443;
					break;
				default:
					throw new ParameterException(getErrorString(
						value, "ServerInfo's port, unknown default port for given scheme"
					));
			}
		}
		return new ServerInfo(scheme, host, port);
	}
}
