package com.mediatoolkit.pareco.exceptions;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 05/11/2018
 */
public class ParecoException extends RuntimeException {

	public ParecoException() {
	}

	public ParecoException(String message) {
		super(message);
	}

	public ParecoException(String message, Throwable cause) {
		super(message, cause);
	}
}
