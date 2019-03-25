package com.mediatoolkit.pareco.transfer;

import com.mediatoolkit.pareco.exceptions.ParecoException;
import com.mediatoolkit.pareco.exceptions.ParecoRuntimeException;
import com.mediatoolkit.pareco.progress.TransferProgressListener;
import com.mediatoolkit.pareco.progress.TransferProgressListenerFactory;
import com.mediatoolkit.pareco.progress.log.LoggingAppender;
import com.mediatoolkit.pareco.transfer.download.DownloadTransferExecutor;
import com.mediatoolkit.pareco.transfer.exit.TransferAbortTrigger;
import com.mediatoolkit.pareco.transfer.model.TransferJob;
import com.mediatoolkit.pareco.transfer.model.TransferTask;
import com.mediatoolkit.pareco.transfer.upload.UploadTransferExecutor;
import java.io.IOException;
import java.util.concurrent.CompletionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 2019-03-23
 */
@Service
public class TransferService {

	private final TransferProgressListenerFactory progressListenerFactory;
	private final UploadTransferExecutor uploadExecutor;
	private final DownloadTransferExecutor downloadExecutor;

	@Autowired
	public TransferService(
		TransferProgressListenerFactory progressListenerFactory,
		UploadTransferExecutor uploadExecutor,
		DownloadTransferExecutor downloadExecutor
	) {
		this.progressListenerFactory = progressListenerFactory;
		this.uploadExecutor = uploadExecutor;
		this.downloadExecutor = downloadExecutor;
	}

	public void execTransfer(
		TransferJob transferJob,
		LoggingAppender loggingAppender,
		TransferAbortTrigger abortTrigger
	) throws ParecoException {
		TransferProgressListener progressListener = progressListenerFactory.createTransferProgressListener(
			transferJob.getLoggingLevel(),
			!transferJob.isNoTransferStats(),
			loggingAppender
		);
		TransferTask transferTask = transferJob.getTransferTask();
		try {
			switch (transferJob.getTransferMode()) {
				case upload:
					uploadExecutor.executeUpload(
						transferTask, abortTrigger, progressListener
					);
					break;
				case download:
					downloadExecutor.executeDownload(
						transferTask, abortTrigger, progressListener
					);
					break;
			}
		} catch (IOException | RestClientException | CompletionException ex) {
			throw new ParecoRuntimeException(ex);
		}
	}
}
