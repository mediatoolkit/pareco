package com.mediatoolkit.pareco.servercomponents;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;




/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 06/11/2018
 */
@Component
@Slf4j
public class ServerInitializedListener implements ApplicationListener<ServletWebServerInitializedEvent> {

	@Override
	public void onApplicationEvent(ServletWebServerInitializedEvent event) {
		int port = event.getWebServer().getPort();
		log.info("---------------------------------------------");
		log.info("Server is listening for requests, port: {}", port);
		log.info("---------------------------------------------");
	}
}
