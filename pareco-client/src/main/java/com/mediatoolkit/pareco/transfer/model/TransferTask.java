package com.mediatoolkit.pareco.transfer.model;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 29/10/2018
 */
@Value
@Builder
@Wither
public class TransferTask {

	private String localRootDirectory;
	private String remoteRootDirectory;
	private String include;	//nullable
	private String exclude;	//nullable
	private ServerInfo serverInfo;
	private String authToken;
	private TransferOptions options;

}
