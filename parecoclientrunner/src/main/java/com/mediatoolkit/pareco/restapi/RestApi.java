package com.mediatoolkit.pareco.restapi;

import com.mediatoolkit.pareco.commandline.ServerInfoConverter;
import com.mediatoolkit.pareco.components.DirectoryStructureReader;
import com.mediatoolkit.pareco.components.TransferNamesEncoding;
import com.mediatoolkit.pareco.model.DirectoryStructure;
import com.mediatoolkit.pareco.restclient.ListClient;
import com.mediatoolkit.pareco.service.LogEventsCollection.LogEvents;
import com.mediatoolkit.pareco.service.TransferRunner;
import com.mediatoolkit.pareco.service.TransferRunner.TransferInfo;
import com.mediatoolkit.pareco.transfer.model.ServerInfo;
import com.mediatoolkit.pareco.transfer.model.TransferJob;
import com.mediatoolkit.pareco.transfer.model.TransferTask;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 2019-03-23
 */
@RestController
@Slf4j
@RequestMapping("/api")
public class RestApi {

	private final TransferRunner transferRunner;
	private final DirectoryStructureReader directoryReader;
	private final TransferNamesEncoding transferNamesEncoding;

	@Autowired
	public RestApi(
		TransferRunner transferRunner,
		DirectoryStructureReader directoryReader,
		TransferNamesEncoding transferNamesEncoding
	) {
		this.transferRunner = transferRunner;
		this.directoryReader = directoryReader;
		this.transferNamesEncoding = transferNamesEncoding;
	}

	@RequestMapping("/submit")
	public String submitTask(
		@RequestBody TransferJob transferJob
	) {
		String transferId = transferRunner.submitTransferJob(transferJob);
		TransferTask transferTask = transferJob.getTransferTask();
		log.info("Got new transfer [{}] {} {} local:{} remote:{}",
			transferId, transferJob.getTransferMode(), transferTask.getServerInfo(),
			transferTask.getLocalRootDirectory(), transferTask.getRemoteRootDirectory()
		);
		return transferId;
	}

	@RequestMapping("/transfers")
	public List<TransferInfo> getTransfers() {
		return transferRunner.listTransferInfos();
	}

	@RequestMapping("/transfers/{transferId}")
	public TransferInfo getTransfer(
		@PathVariable String transferId
	) {
		return transferRunner.getTransfer(transferId).toTransferInfo();
	}

	@RequestMapping("/transfers/{transferId}/log")
	public LogEvents getTransferLog(
		@PathVariable String transferId,
		@RequestParam("from") int from
	) {
		return transferRunner.getTransfer(transferId).getLogEvents().getFrom(from, 100);
	}

	@RequestMapping("/transfers/{transferId}/abort")
	public void abortTransfer(
		@PathVariable String transferId
	) {
		log.info("Aborting transfer [{}]", transferId);
		transferRunner.getTransfer(transferId).abort();
	}

	@RequestMapping("/checkLocalDir")
	public DirectoryStructure checkLocalDir(
		@RequestParam("localDir") String localRootDir,
		@RequestParam(name = "include", required = false) String include,
		@RequestParam(name = "exclude", required = false) String exclude
	) throws IOException {
		return directoryReader.readDirectoryStructure(
			localRootDir,
			"".equals(include) ? null : include,
			"".equals(exclude) ? null : exclude
		);
	}

	@RequestMapping("/checkRemoteDir")
	public DirectoryStructure checkRemoteDir(
		@RequestParam("server") String server,
		@RequestParam("remoteDir") String remoteRootDir,
		@RequestParam(name = "include", required = false) String include,
		@RequestParam(name = "exclude", required = false) String exclude,
		@RequestParam(name = "authToken", required = false) String authToken
	) {
		ServerInfo serverInfo = new ServerInfoConverter("server").convert(server);
		ListClient listClient = ListClient.builder()
			.httpScheme(serverInfo.getHttpScheme())
			.host(serverInfo.getHost())
			.port(serverInfo.getPort())
			.authToken("".equals(authToken) ? null : authToken)
			.connectTimeout(5_000)	//TODO use data from form
			.readTimeout(120_000)	//TODO use data from form
			.encoding(transferNamesEncoding)
			.build();
		return listClient.listDirectory(
			remoteRootDir,
			"".equals(include) ? null : include,
			"".equals(exclude) ? null : exclude
		);
	}

}
