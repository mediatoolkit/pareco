package com.mediatoolkit.pareco;

import com.mediatoolkit.pareco.commandline.RunnerCommandLineOptions;
import com.mediatoolkit.pareco.util.commandline.CommandLineUtil;
import java.util.Optional;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 2019-03-23
 */
@SpringBootApplication
@Slf4j
public class ParecoClientRunner {

	public static void main(String[] args) {
		Optional<RunnerCommandLineOptions> optOptions = CommandLineUtil.readOptions(
			RunnerCommandLineOptions.class, args
		);
		if (!optOptions.isPresent()) {
			return;
		}
		RunnerCommandLineOptions options = optOptions.get();
		Properties properties = new Properties();
		properties.setProperty("server.port", String.valueOf(options.getPort()));
		if (options.getServer() != null) {
			properties.setProperty("fixed-parameter.server.scheme", options.getServer().getHttpScheme());
			properties.setProperty("fixed-parameter.server.host", options.getServer().getHost());
			properties.setProperty("fixed-parameter.server.port", Integer.toString(options.getServer().getPort()));
		}
		if (options.getAuthToken() != null) {
			properties.setProperty("fixed-parameter.auth-token", options.getAuthToken());
		}
		if (options.getLocalDir() != null) {
			properties.setProperty("fixed-parameter.local-dir", options.getLocalDir());
		}
		if (options.getRemoteDir() != null) {
			properties.setProperty("fixed-parameter.remote-dir", options.getRemoteDir());
		}
		new SpringApplicationBuilder(ParecoClientRunner.class)
			.properties(properties)
			.run(args);
	}
}
