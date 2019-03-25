package com.mediatoolkit.pareco;

import com.mediatoolkit.pareco.commandline.RunnerCommandLineOptions;
import com.mediatoolkit.pareco.util.commandline.CommandLineUtil;
import java.util.Optional;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

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
		new SpringApplicationBuilder(ParecoClientRunner.class)
			.properties(properties)
			.run(args);
	}
}
