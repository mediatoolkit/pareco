package com.mediatoolkit.pareco;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.mediatoolkit.pareco.commandline.CommandLineOptions;
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
		CommandLineOptions options;
		try {
			options = new CommandLineOptions();
			JCommander jCommander = JCommander.newBuilder()
				.addObject(options)
				.programName("./pareco-server.sh")
				.build();
			jCommander.parse(args);
			if (options.isHelp()) {
				jCommander.usage();
				return;
			}
			if (options.isVersion()) {
				System.out.println("Version: " + Version.getVersion());
				return;
			}
			options.validate();
		} catch (ParameterException ex) {
			log.error("Command line parameter exception: {}", ex.getMessage());
			return;
		}
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
