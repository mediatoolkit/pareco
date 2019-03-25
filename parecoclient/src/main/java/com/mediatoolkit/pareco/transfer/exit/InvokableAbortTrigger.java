package com.mediatoolkit.pareco.transfer.exit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 2019-03-24
 */
public class InvokableAbortTrigger implements TransferAbortTrigger {

	private List<Abortable> abortables = Collections.synchronizedList(new ArrayList<>());

	@Override
	public void registerAbort(Abortable abortable) {
		abortables.add(abortable);
	}

	public void triggerAbort() {
		abortables.forEach(Abortable::abort);
	}
}
