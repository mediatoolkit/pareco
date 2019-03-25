package com.mediatoolkit.pareco.util.commandline;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.mediatoolkit.pareco.Version;
import java.util.Optional;
import static java.util.Optional.empty;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 2019-03-25
 */
@Slf4j
public class CommandLineUtil {

	public static <T extends CommandLineOptions> Optional<T> readOptions(
		Class<T> optionsClass, String... args
	) {
		T options;
		try {
			options = optionsClass.newInstance();
			JCommander jCommander = JCommander.newBuilder()
				.addObject(options)
				.programName("./pareco-cli.sh")
				.build();
			jCommander.parse(args);
			if (options.isHelp()) {
				jCommander.usage();
				return empty();
			}
			if (options.isVersion()) {
				System.out.println("Version: " + Version.getVersion());
				return empty();
			}
			options.validate();
		} catch (ParameterException ex) {
			log.error("Command line parameter exception: {}", ex.toString());
			return empty();
		} catch (Exception e) {
			log.error("Unexpected exception: ", e);
			return empty();
		}
		return Optional.of(options);
	}
}
