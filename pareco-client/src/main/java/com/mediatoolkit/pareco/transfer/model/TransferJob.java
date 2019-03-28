package com.mediatoolkit.pareco.transfer.model;

import com.mediatoolkit.pareco.progress.TransferLoggingLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 2019-03-23
 */
@Value
@Builder
public class TransferJob {

	@NonNull
	private TransferMode transferMode;
	@NonNull
	private TransferTask transferTask;
	@NonNull
	private TransferLoggingLevel loggingLevel;
	private boolean noTransferStats;
	private boolean forceColors;
}
