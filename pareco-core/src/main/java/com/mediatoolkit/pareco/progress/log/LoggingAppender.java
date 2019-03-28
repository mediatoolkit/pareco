package com.mediatoolkit.pareco.progress.log;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 2019-03-23
 */
public interface LoggingAppender {

	void addMsg(String marker, Message msg);

	static LoggingAppender compose(LoggingAppender... loggingAppenders) {
		return (marker, msg) -> {
			for (LoggingAppender appender : loggingAppenders) {
				appender.addMsg(marker, msg);
			}
		};
	}


}
