package com.mediatoolkit.pareco.progress.log;

import com.mediatoolkit.pareco.progress.log.Message.Part;
import org.fusesource.jansi.Ansi;
import static org.fusesource.jansi.Ansi.ansi;
import org.fusesource.jansi.AnsiConsole;
import org.slf4j.Logger;
import org.slf4j.MarkerFactory;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 2019-03-23
 */
public class Slf4jLoggingAppender implements LoggingAppender {

	private final Logger log;

	public Slf4jLoggingAppender(String loggerName, boolean forceColors) {
		log = org.slf4j.LoggerFactory.getLogger(loggerName);
		if (forceColors) {
			System.setProperty("jansi.force", "true");
		}
		AnsiConsole.systemInstall();
	}

	@Override
	public void addMsg(String marker, Message msg) {
		log.info(
			MarkerFactory.getMarker(marker),
			ansiColorize(msg)
		);
	}

	private String ansiColorize(Message msg) {
		Ansi ansi = ansi();
		for (Part part : msg.getFlatParts()) {
			String value = part.getValue();
			switch (part.getFormat()) {
				case NONE:
					ansi.reset().a(value);
					break;
				case HIGHLIGHT:
					ansi.fgBrightRed().a(value);
					break;
				case LITERAL:
					ansi.fgBrightBlue().a(value);
					break;
				case SPEED:
					ansi.fgBrightGreen().a(value);
					break;
				case CHUNK:
					ansi.fgBlack().a(value);
					break;
				case FILE:
					ansi.fgBrightCyan().a(value);
					break;
				case DELETE:
					ansi.fgBrightYellow().a(value);
					break;
			}
		}
		return ansi.reset().toString();
	}
}
