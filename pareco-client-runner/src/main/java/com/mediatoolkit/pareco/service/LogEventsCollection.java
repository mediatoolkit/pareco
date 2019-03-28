package com.mediatoolkit.pareco.service;

import com.mediatoolkit.pareco.progress.log.LoggingAppender;
import com.mediatoolkit.pareco.progress.log.Message;
import java.util.ArrayList;
import static java.util.Collections.emptyList;
import java.util.Date;
import java.util.List;
import lombok.Value;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 2019-03-24
 */
public class LogEventsCollection implements LoggingAppender {

	private final List<LogEvent> events = new ArrayList<>();

	@Override
	public void addMsg(String marker, Message msg) {
		LogEvent event = new LogEvent(new Date(), Thread.currentThread().getName(), marker, msg);
		synchronized (events) {
			events.add(event);
		}
	}

	public LogEvents getFrom(int fromIndex, int limit) {
		synchronized (events) {
			if (fromIndex > events.size()) {
				return LogEvents.empty(events.size());
			}
			int toIndex = Math.min(fromIndex + limit, events.size());
			return new LogEvents(
				fromIndex,
				toIndex,
				new ArrayList<>(events.subList(fromIndex, toIndex))
			);
		}
	}

	@Value
	public static class LogEvent {

		private Date date;
		private String thread;
		private String marker;
		private Message msg;
	}

	@Value
	public static class LogEvents {

		private int fromIndex;
		private int toIndex;
		private List<LogEvent> list;

		public static LogEvents empty(int fromIndex) {
			return new LogEvents(fromIndex, fromIndex, emptyList());
		}
	}
}
