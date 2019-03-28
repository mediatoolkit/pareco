package com.mediatoolkit.pareco.transfer.model;

import lombok.NonNull;
import lombok.Value;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 28/10/2018
 */
@Value
public class ServerInfo {

	@NonNull
	private String httpScheme;
	@NonNull
	private String host;
	private int port;

	public String toUrl() {
		return httpScheme + "://" + host + portStr();
	}

	@Override
	public String toString() {
		return toUrl();
	}

	private String portStr() {
		if ("http".equals(httpScheme) && port == 80) {
			return "";
		}
		if ("https".equals(httpScheme) && port == 443) {
			return "";
		}
		return ":" + port;
	}
}
