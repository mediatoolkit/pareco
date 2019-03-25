package com.mediatoolkit.pareco;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.mediatoolkit.pareco.commandline.RunnerCommandLineOptions;
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
		RunnerCommandLineOptions options;
		try {
			options = new RunnerCommandLineOptions();
			JCommander jCommander = JCommander.newBuilder()
				.addObject(options)
				.programName("./pareco-server.sh")
				.build();
			jCommander.parse(args);
			if (options.isHelp()) {
				jCommander.usage();
				return;
			}
			options.validate();
		} catch (ParameterException ex) {
			log.error("Command line parameter exception: {}", ex.getMessage());
			return;
		}
		Properties properties = new Properties();
		properties.setProperty("server.port", String.valueOf(options.getPort()));
		new SpringApplicationBuilder(ParecoClientRunner.class)
			.properties(properties)
			.run(args);
	}
}
