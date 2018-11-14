package com.mediatoolkit.pareco.auth;

import lombok.NonNull;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 03/11/2018
 */
public interface Authenticator {

	void authenticate(String authToken) throws AuthenticationExceptipn;

	Authenticator NO_AUTH = authToken -> {
		if (authToken != null) {
			throw new AuthenticationExceptipn("AuthToken is received, expecting no token");
		}
	};

	static Authenticator withToken(@NonNull String authToken) {
		return token -> {
			if (token == null) {
				throw new AuthenticationExceptipn("Valid authToken is required, got null");
			}
			if (!authToken.equals(token)) {
				throw new AuthenticationExceptipn("Given authToken is invalid");
			}
		};
	}
}
