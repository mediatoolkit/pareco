package com.mediatoolkit.pareco.exceptions;

import com.mediatoolkit.pareco.model.ErrorBody;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 05/11/2018
 */
public abstract class ParecoException extends RuntimeException {

	public ParecoException() {
	}

	public ParecoException(String message) {
		super(message);
	}

	public ParecoException(Throwable cause) {
		super(cause);
	}

	public ParecoException(String message, Throwable cause) {
		super(message, cause);
	}

	public abstract ErrorBody.Type type();
}
