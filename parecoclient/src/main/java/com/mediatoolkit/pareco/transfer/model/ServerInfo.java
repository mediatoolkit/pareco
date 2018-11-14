package com.mediatoolkit.pareco.transfer.model;

import lombok.Value;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 28/10/2018
 */
@Value
public class ServerInfo {

	private String httpScheme;
	private String host;
	private int port;
}
