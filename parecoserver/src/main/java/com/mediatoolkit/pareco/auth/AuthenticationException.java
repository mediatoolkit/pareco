package com.mediatoolkit.pareco.auth;

import com.mediatoolkit.pareco.exceptions.ParecoException;
import com.mediatoolkit.pareco.model.ErrorBody.Type;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 03/11/2018
 */
public class AuthenticationException extends ParecoException {

	public AuthenticationException(String message) {
		super(message);
	}

	@Override
	public Type type() {
		return Type.ILLEGAL_STATE;
	}
}
