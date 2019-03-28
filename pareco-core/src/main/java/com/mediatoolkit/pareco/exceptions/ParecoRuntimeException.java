package com.mediatoolkit.pareco.exceptions;

import com.mediatoolkit.pareco.model.ErrorBody.Type;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 2019-03-24
 */
public class ParecoRuntimeException extends ParecoException {

	public ParecoRuntimeException(Throwable cause) {
		super(cause);
	}

	@Override
	public Type type() {
		return Type.UNKNOWN;
	}
}
