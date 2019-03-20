package com.mediatoolkit.pareco.progress;

import com.mediatoolkit.pareco.progress.LoggingTransferProgressListener.LoggingFilter;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 03/11/2018
 */
@Component
public class TransferProgressListenerFactory {

	public TransferProgressListener createTransferProgressListener(
		TransferLoggingLevel loggingLevel, boolean stats, boolean forceColors
	) {
		StatsRecordingTransferProgressListener statsProgressListener = stats
			? new StatsRecordingTransferProgressListener()
			: null;
		Speedometer speedometer = new Speedometer();
		switch (loggingLevel) {
			case OFF:
				return TransferProgressListener.NO_OP_LISTENER;
			case QUIET:
				return LoggingTransferProgressListener.builder()
					.forceColors(forceColors)
					.loggingFilter(LoggingFilter.START_END)
					.build();
			case STATS_NO_SPEED:
				return CompositeTransferProgressListener.of(
					statsProgressListener, LoggingTransferProgressListener.builder()
						.forceColors(forceColors)
						.statsListener(statsProgressListener)
						.loggingFilter(LoggingFilter.START_END)
						.build()
				);
			case STATS:
				return CompositeTransferProgressListener.of(
					statsProgressListener, LoggingTransferProgressListener.builder()
						.forceColors(forceColors)
						.statsListener(statsProgressListener)
						.speedometer(speedometer)
						.loggingFilter(LoggingFilter.START_END_SPEED)
						.build()
				);
			case FILES_NO_SPEED:
				return CompositeTransferProgressListener.of(
					statsProgressListener, LoggingTransferProgressListener.builder()
						.forceColors(forceColors)
						.loggingFilter(LoggingFilter.FILES)
						.statsListener(statsProgressListener)
						.build()
				);
			case FILES:
				return CompositeTransferProgressListener.of(
					statsProgressListener, LoggingTransferProgressListener.builder()
						.forceColors(forceColors)
						.loggingFilter(LoggingFilter.FILES_SPEED)
						.speedometer(speedometer)
						.statsListener(statsProgressListener)
						.build()
				);
			case CHUNKS:
				return CompositeTransferProgressListener.of(
					statsProgressListener, LoggingTransferProgressListener.builder()
						.forceColors(forceColors)
						.loggingFilter(LoggingFilter.CHUNKS)
						.speedometer(speedometer)
						.statsListener(statsProgressListener)
						.build()
				);
			default:
				throw new UnsupportedOperationException("Unknown logging level: " + loggingLevel);
		}
	}


}
