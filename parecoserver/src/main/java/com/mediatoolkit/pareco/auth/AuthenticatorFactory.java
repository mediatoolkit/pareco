package com.mediatoolkit.pareco.auth;

import java.security.SecureRandom;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 03/11/2018
 */
@Slf4j
@Component
public class AuthenticatorFactory extends AbstractFactoryBean<Authenticator> {


	@Value("${auth.token:#{null}}")
	private String authToken;

	@Value("${auth.token.generate:#{false}}")
	private boolean generate;

	@Override
	public Class<?> getObjectType() {
		return Authenticator.class;
	}

	@Override
	protected Authenticator createInstance() {
		if (generate) {
			String generatedAuthToken = generateAndPrintToken();
			return Authenticator.withToken(generatedAuthToken);
		}
		if (authToken == null) {
			return Authenticator.NO_AUTH;
		} else {
			return Authenticator.withToken(authToken);
		}
	}

	private String generateAndPrintToken() {
		SecureRandom random = new SecureRandom();
		byte[] token = new byte[24];
		random.nextBytes(token);
		String authToken = Hex.encodeHexString(token);
		log.info("*********************************************************************");
		log.info("Generated authToken: {}", authToken);
		log.info("*********************************************************************");
		return authToken;
	}
}
