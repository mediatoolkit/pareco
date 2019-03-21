package com.mediatoolkit.pareco.exceptions;

import com.mediatoolkit.pareco.model.ErrorBody.Type;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 25/10/2018
 */
public class InputStreamSizeMissMatchException extends ParecoException {

	public InputStreamSizeMissMatchException(String s) {
		super(s);
	}

	@Override
	public Type type() {
		return Type.ILLEGAL_STATE;
	}

}
