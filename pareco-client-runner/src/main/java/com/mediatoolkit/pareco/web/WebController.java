package com.mediatoolkit.pareco.web;

import com.mediatoolkit.pareco.commandline.ChunkSizeConverter;
import com.mediatoolkit.pareco.commandline.ClientCommandLineOptions;
import com.mediatoolkit.pareco.commandline.ClientCommandLineOptions.ClientCommandLineOptionsBuilder;
import com.mediatoolkit.pareco.commandline.ServerInfoConverter;
import com.mediatoolkit.pareco.commandline.TimeoutConverter;
import com.mediatoolkit.pareco.model.DigestType;
import com.mediatoolkit.pareco.progress.TransferLoggingLevel;
import com.mediatoolkit.pareco.service.StartupParameters;
import com.mediatoolkit.pareco.service.TransferRunner;
import com.mediatoolkit.pareco.service.TransferRunner.Transfer;
import com.mediatoolkit.pareco.transfer.model.TransferJob;
import com.mediatoolkit.pareco.transfer.model.TransferMode;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.EntryStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 2019-03-24
 */
@Controller
@Slf4j
public class WebController {

	private final TransferRunner transferRunner;
	private final StartupParameters startupParameters;

	@Autowired
	public WebController(
		TransferRunner transferRunner,
		StartupParameters startupParameters
	) {
		this.transferRunner = transferRunner;
		this.startupParameters = startupParameters;
	}

	@RequestMapping("/transfers")
	public ModelAndView transfersView() {
		return new ModelAndView(
			"transfers", "transferInfos",
			transferRunner.listTransferInfos()
		);
	}

	@RequestMapping("/transfers/createNew")
	public ModelAndView createNewTransfer() {
		return new ModelAndView("createNewTransfer",
			EntryStream.of(
				"digestTypes", DigestType.values(),
				"logLevels", TransferLoggingLevel.values(),
				"parameters", startupParameters
			).toMap()
		);
	}

	@RequestMapping("/transfers/{transferId}/createNew")
	public ModelAndView createNewTransfer(
		@PathVariable("transferId") String transferId
	) {
		Transfer transfer = transferRunner.getTransfer(transferId);
		return new ModelAndView("createNewTransfer",
			EntryStream.of(
				"job", transfer.getTransferJob(),
				"digestTypes", DigestType.values(),
				"logLevels", TransferLoggingLevel.values(),
				"parameters", startupParameters
			).toMap()
		);
	}

	@RequestMapping("/transfers/{transferId}")
	public ModelAndView viewTransfer(
		@PathVariable("transferId") String transferId
	) {
		Transfer transfer = transferRunner.getTransfer(transferId);
		return new ModelAndView(
			"transfer", "transferInfo", transfer.toTransferInfo()
		);
	}

	@PostMapping("/transfers/submitCreateNew")
	public RedirectView submitNewTransfer(
		@RequestParam("mode") TransferMode transferMode,
		@RequestParam("server") String server,
		@RequestParam("remoteDir") String remoteDir,
		@RequestParam("localDir") String localDir,
		@RequestParam(name = "include", required = false) String include,
		@RequestParam(name = "exclude", required = false) String exclude,
		@RequestParam(name = "numConnections", required = false) Integer numTransferConnections,
		@RequestParam(name = "timeout", required = false) String timeout,
		@RequestParam(name = "connectTimeout", required = false) String connectTimeout,
		@RequestParam(name = "deleteUnexpected", required = false) String deleteUnexpected,
		@RequestParam(name = "chunkSize", required = false) String chunkSize,
		@RequestParam(name = "digestType", required = false) DigestType digestType,
		@RequestParam(name = "skipDigest", required = false) String skipDigest,
		@RequestParam(name = "authToken", required = false) String authToken,
		@RequestParam(name = "logLevel", required = false) TransferLoggingLevel logLevel
	) {
		ClientCommandLineOptionsBuilder builder = new ClientCommandLineOptions().toBuilder()
			.mode(transferMode)
			.server(new ServerInfoConverter("server").convert(server))
			.localDir(localDir)
			.remoteDir(remoteDir)
			.forceColors(true);
		if (include != null && !include.isEmpty()) {
			builder.include(include);
		}
		if (exclude != null && !exclude.isEmpty()) {
			builder.exclude(exclude);
		}
		if (numTransferConnections != null) {
			builder.numTransferConnections(numTransferConnections);
		}
		if (timeout != null) {
			builder.timeout(new TimeoutConverter("timeout").convert(timeout));
		}
		if (connectTimeout != null) {
			builder.connectTimeout(new TimeoutConverter("connectTimeout").convert(connectTimeout));
		}
		if (deleteUnexpected != null) {
			builder.deleteUnexpected(true);
		}
		if (chunkSize != null) {
			builder.chunkSizeBytes(new ChunkSizeConverter("chunkSize").convert(chunkSize));
		}
		if (digestType != null) {
			builder.digestType(digestType);
		}
		if (skipDigest != null) {
			builder.skipDigestCheck(true);
		}
		if (authToken != null && !authToken.isEmpty()) {
			builder.authToken(authToken);
		}
		if (logLevel != null) {
			builder.loggingLevel(logLevel);
		}
		ClientCommandLineOptions commandLineOptions = builder.build();
		commandLineOptions.validate();
		TransferJob transferJob = commandLineOptions.toTransferJob();
		String transferId = transferRunner.submitTransferJob(transferJob);
		log.info("Got new transfer [{}] {} {} local:{} remote:{}",
			transferId, transferMode, server, localDir, remoteDir
		);
		return new RedirectView("/transfers/" + transferId);
	}

	@PostMapping("/transfers/{transferId}/abort")
	public RedirectView abortTransfer(
		@PathVariable("transferId") String transferId
	) {
		transferRunner.getTransfer(transferId).abort();
		return new RedirectView("/transfers/" + transferId);
	}

	@PostMapping("/transfers/{transferId}/reCreate")
	public RedirectView reCreateTransfer(
		@PathVariable("transferId") String transferId
	) {
		TransferJob transferJob = transferRunner.getTransfer(transferId).getTransferJob();
		String newTransferId = transferRunner.submitTransferJob(transferJob);
		return new RedirectView("/transfers/" + newTransferId);
	}
}
