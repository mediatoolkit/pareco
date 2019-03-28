package com.mediatoolkit.pareco.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 06/11/2018
 */
@Component
public class TomcatWebServerCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

	@Override
	public void customize(TomcatServletWebServerFactory factory) {
		factory.addConnectorCustomizers(connector -> connector.setAttribute("relaxedQueryChars", "[]{}|"));
	}
}
