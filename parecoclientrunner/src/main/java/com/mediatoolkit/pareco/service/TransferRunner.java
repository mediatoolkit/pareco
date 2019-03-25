package com.mediatoolkit.pareco.service;

import com.mediatoolkit.pareco.progress.log.LoggingAppender;
import com.mediatoolkit.pareco.progress.log.Slf4jLoggingAppender;
import com.mediatoolkit.pareco.transfer.TransferService;
import com.mediatoolkit.pareco.transfer.exit.InvokableAbortTrigger;
import com.mediatoolkit.pareco.transfer.model.TransferJob;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import static java.util.stream.Collectors.toList;
import lombok.Getter;
import lombok.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 2019-03-23
 */
@Service
public class TransferRunner implements AutoCloseable {

	private final ThreadFactory threadFactory = new CustomizableThreadFactory("TransferExecutor");
	private final TransferService transferService;
	private final ExecutorService executor;
	private final Map<String, Transfer> transfers = Collections.synchronizedMap(new LinkedHashMap<>());
	private final Object abortLock = new Object();

	@Autowired
	public TransferRunner(
		TransferService transferService
	) {
		this.transferService = transferService;
		this.executor = Executors.newSingleThreadExecutor(threadFactory);
	}

	@Override
	public void close() {
		executor.shutdown();
	}

	public String submitTransferJob(TransferJob transferJob) {
		String transferId = UUID.randomUUID().toString();
		Transfer transfer = new Transfer();
		transfer.transferJob = transferJob;
		transfer.id = transferId;
		transfer.state = TransferState.PENDING;
		transfer.logEvents = new LogEventsCollection();
		transfer.abortTrigger = new InvokableAbortTrigger();
		LoggingAppender loggingAppender = LoggingAppender.compose(
			new Slf4jLoggingAppender("Transfer", transferJob.isForceColors()),
			transfer.logEvents
		);
		transfer.execution = executor.submit(() -> {
			synchronized (abortLock) {
				if (transfer.state == TransferState.ABORTED) {
					return;
				}
			}
			transfer.state = TransferState.EXECUTING;
			try {
				transferService.execTransfer(transferJob, loggingAppender, transfer.abortTrigger);
				transfer.state = TransferState.COMPLETED;
			} catch (Throwable throwable) {
				if (transfer.state != TransferState.ABORTED) {
					transfer.failCause = throwable;
					transfer.state = TransferState.FAILED;
				}
			}
		});
		transfers.put(transferId, transfer);
		return transferId;
	}

	public Transfer getTransfer(String transferId) {
		Transfer transfer = transfers.get(transferId);
		if (transfer == null) {
			throw new MissingTransferException(transferId);
		}
		return transfer;
	}

	public List<Transfer> listTransfers() {
		return new ArrayList<>(transfers.values());
	}

	public List<TransferInfo> listTransferInfos() {
		return listTransfers().stream()
			.map(Transfer::toTransferInfo)
			.collect(toList());
	}

	public enum TransferState {
		PENDING, EXECUTING, COMPLETED, FAILED, ABORTING, ABORTED
	}

	@Getter
	public class Transfer {

		private Future<?> execution;
		private String id;
		private TransferJob transferJob;

		private TransferState state;
		private Throwable failCause;

		private LogEventsCollection logEvents;
		private InvokableAbortTrigger abortTrigger;

		public void abort() {
			synchronized (abortLock) {
				if (state == TransferState.PENDING) {
					execution.cancel(false);
					state = TransferState.ABORTED;
					return;
				}
			}
			if (execution.isDone()) {
				return;
			}
			state = TransferState.ABORTING;
			abortTrigger.triggerAbort();
		}

		public TransferInfo toTransferInfo() {
			return new TransferInfo(
				id, state, failCause == null ? null : failCause.toString(), transferJob
			);
		}

	}

	@Value
	public static class TransferInfo {

		private String id;
		private TransferState state;
		private String failCause;

		private TransferJob transferJob;
	}

	public static class MissingTransferException extends RuntimeException {

		public MissingTransferException(String transferId) {
			super("No transfer with id: " + transferId);
		}
	}

}


