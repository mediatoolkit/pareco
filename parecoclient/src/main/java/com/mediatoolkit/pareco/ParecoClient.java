package com.mediatoolkit.pareco;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.mediatoolkit.pareco.commandline.CommandLineOptions;
import com.mediatoolkit.pareco.exceptions.ParecoException;
import com.mediatoolkit.pareco.progress.TransferProgressListener;
import com.mediatoolkit.pareco.progress.TransferProgressListenerFactory;
import com.mediatoolkit.pareco.transfer.download.DownloadTransferExecutor;
import com.mediatoolkit.pareco.transfer.model.TransferTask;
import com.mediatoolkit.pareco.transfer.upload.UploadTransferExecutor;
import java.io.IOException;
import java.util.concurrent.CompletionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.client.RestClientException;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 31/10/2018
 */
@SpringBootApplication
@Slf4j
public class ParecoClient {

	public static void main(String[] args) {
		CommandLineOptions options;
		try {
			options = new CommandLineOptions();
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
		TransferTask transferTask = options.toTransferTask();
		try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(ParecoClient.class)
			.logStartupInfo(false)
			.bannerMode(Mode.OFF)
			.run(args)
		) {
			TransferProgressListener progressListener = ctx.getBean(TransferProgressListenerFactory.class)
				.createTransferProgressListener(
					options.getLoggingLevel(), !options.isNoTransferStats()
				);
			switch (options.getMode()) {
				case upload:
					ctx.getBean(UploadTransferExecutor.class).executeUpload(transferTask, progressListener);
					break;
				case download:
					ctx.getBean(DownloadTransferExecutor.class).executeDownload(transferTask, progressListener);
					break;
			}
		} catch (IOException | RestClientException | ParecoException | CompletionException ex) {
			log.warn("Transfer failed");
			log.error("Fail cause: {}", ex.toString());
		}
	}

}
