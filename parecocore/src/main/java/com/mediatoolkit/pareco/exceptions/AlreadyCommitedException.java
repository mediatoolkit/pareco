package com.mediatoolkit.pareco.exceptions;

import com.mediatoolkit.pareco.model.ErrorBody.Type;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 25/10/2018
 */
public class AlreadyCommitedException extends ParecoException {

	public AlreadyCommitedException(String message) {
		super(message);
	}

	@Override
	public Type type() {
		return Type.ILLEGAL_STATE;
	}
}
