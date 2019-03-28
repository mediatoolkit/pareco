package com.mediatoolkit.pareco.transfer.exit;

import com.mediatoolkit.pareco.util.Util;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 08/11/2018
 */
public class JvmExitAbortTrigger implements TransferAbortTrigger {

	@Override
	public void registerAbort(Abortable abortable) {
		Runtime.getRuntime().addShutdownHook(new Thread(
			() -> Util.runIgnoreException(abortable::abort)
		));
	}
}
