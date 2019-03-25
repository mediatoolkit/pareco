package com.mediatoolkit.pareco;

import com.mediatoolkit.pareco.commandline.ServerCommandLineOptions;
import com.mediatoolkit.pareco.util.commandline.CommandLineUtil;
import java.util.Optional;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 29/10/2018
 */
@SpringBootApplication
@Slf4j
public class ParecoServer {

	public static void main(String[] args) {
		Optional<ServerCommandLineOptions> optOptions = CommandLineUtil.readOptions(
			ServerCommandLineOptions.class, args
		);
		if (!optOptions.isPresent()) {
			return;
		}
		ServerCommandLineOptions options = optOptions.get();
		Properties properties = new Properties();
		properties.setProperty("server.port", String.valueOf(options.getPort()));
		properties.setProperty("auth.token.generate", String.valueOf(options.isGenerateToken()));
		properties.setProperty("session.expire.max_inactive", String.valueOf(options.getSessionExpire()));
		if (options.getAuthToken() != null) {
			properties.setProperty("auth.token", options.getAuthToken());
		}
		new SpringApplicationBuilder(ParecoServer.class)
			.logStartupInfo(false)
			.bannerMode(Mode.OFF)
			.properties(properties)
			.run(args);
	}
}
