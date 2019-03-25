package com.mediatoolkit.pareco;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.mediatoolkit.pareco.commandline.ClientCommandLineOptions;
import com.mediatoolkit.pareco.exceptions.ParecoException;
import com.mediatoolkit.pareco.progress.log.LoggingAppender;
import com.mediatoolkit.pareco.progress.log.Slf4jLoggingAppender;
import com.mediatoolkit.pareco.transfer.TransferService;
import com.mediatoolkit.pareco.transfer.exit.JvmExitAbortTrigger;
import com.mediatoolkit.pareco.transfer.exit.TransferAbortTrigger;
import com.mediatoolkit.pareco.transfer.model.TransferJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 31/10/2018
 */
@SpringBootApplication
@Slf4j
public class ParecoClient {

	public static void main(String[] args) {
		ClientCommandLineOptions options;
		try {
			options = new ClientCommandLineOptions();
			JCommander jCommander = JCommander.newBuilder()
				.addObject(options)
				.programName("./pareco-cli.sh")
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
			log.error(ex.toString());
			return;
		}
		TransferJob transferJob = options.toTransferJob();
		if (options.isForceColors()) {
			System.setProperty("spring.output.ansi.enabled", "ALWAYS");
		}
		LoggingAppender loggingAppender = new Slf4jLoggingAppender("Transfer", options.isForceColors());
		TransferAbortTrigger abortTrigger = new JvmExitAbortTrigger();
		try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(ParecoClient.class)
			.logStartupInfo(false)
			.bannerMode(Mode.OFF)
			.run(args)
		) {
			ctx.getBean(TransferService.class).execTransfer(
				transferJob, loggingAppender, abortTrigger
			);
		} catch (ParecoException ex) {
			log.warn("Transfer failed");
			log.error("Fail cause:\n{}", ex.toString().replace(":", ":\n    "));
		}
	}

}
