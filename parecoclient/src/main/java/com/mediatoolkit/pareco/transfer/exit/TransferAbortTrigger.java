package com.mediatoolkit.pareco.transfer.exit;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 2019-03-23
 */
public interface TransferAbortTrigger {

	void registerAbort(Abortable abortable);

}
