package com.mediatoolkit.pareco.exceptions;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 25/10/2018
 */
public class AlreadyCommitedException extends ParecoException {

	public AlreadyCommitedException(String message) {
		super(message);
	}
}
