package com.mediatoolkit.pareco.progress.log;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import java.util.List;
import java.util.Objects;
import lombok.NonNull;
import lombok.Value;
import one.util.streamex.StreamEx;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 2019-03-24
 */
@Value
public class Message {

	public enum Format {
		NONE, HIGHLIGHT, LITERAL, SPEED, FILE, CHUNK, DELETE
	}

	@NonNull
	private final Format format;

	private final String value;
	private final List<Message> parts;

	@NonNull
	private final List<Part> flatParts;

	private Message(Format format, String value, List<Message> parts) {
		this.format = format;
		this.value = value;
		this.parts = parts;
		this.flatParts = flatParts(format, value, parts);
	}

	public static Message msg(Object... parts) {
		return msg(Format.NONE, parts);
	}

	public static Message highlight(Object... parts) {
		return msg(Format.HIGHLIGHT, parts);
	}

	public static Message quote(Object literal) {
		return msg(Format.LITERAL, "'" + literal + "'");
	}

	public static Message msg(Format format, @NonNull Object... parts) {
		List<Message> messages = StreamEx.of(parts)
			.map(part -> {
				if (part instanceof Message) {
					return (Message) part;
				} else {
					return new Message(format, Objects.toString(part), null);
				}
			})
			.toList();
		if (messages.size() > 1) {
			return new Message(format, null, messages);
		} else if (messages.isEmpty()) {
			return new Message(format, null, emptyList());
		} else {
			return messages.get(0);
		}
	}

	@Override
	public String toString() {
		return StreamEx.of(flatParts).map(Part::getValue).joining("");
	}

	private static List<Part> flatParts(Format format, String value, List<Message> parts) {
		if (parts == null) {
			return singletonList(Part.of(format, value));
		}
		return StreamEx.of(parts)
			.flatCollection(Message::getFlatParts)
			.toList();
	}

	@Value(staticConstructor = "of")
	public static class Part {

		private Format format;
		private String value;
	}
}
