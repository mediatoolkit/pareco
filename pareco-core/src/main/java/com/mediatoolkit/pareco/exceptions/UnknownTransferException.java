package com.mediatoolkit.pareco.exceptions;

import com.mediatoolkit.pareco.model.ErrorBody.Type;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 2019-03-21
 */
public class UnknownTransferException extends ParecoException {

	public UnknownTransferException(Throwable cause) {
		super(cause);
	}

	@Override
	public Type type() {
		return Type.UNKNOWN;
	}
}
