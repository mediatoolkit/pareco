package com.mediatoolkit.pareco.commandline;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import one.util.streamex.EntryStream;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 02/11/2018
 */
public class ChunkSizeConverter extends BaseConverter<Long> implements IStringConverter<Long> {

	private final Pattern pattern = Pattern.compile("([0-9]+)([bBkKmMgG]?)");
	private final Map<String, Long> factors = EntryStream
		.of(
			"", 1L,
			"B", 1L,
			"K", 1L << 10,
			"M", 1L << 20,
			"G", 1L << 30
		)
		.toMap();

	public ChunkSizeConverter(String optionName) {
		super(optionName);
	}

	@Override
	public Long convert(String value) {
		Matcher matcher = pattern.matcher(value);
		if (!matcher.matches()) {
			throw new ParameterException(getErrorString(value, "numeric chunk size, expected pattern: " + pattern));
		}
		long sizeNum = Long.parseLong(matcher.group(1));
		long factor = factors.get(matcher.group(2));
		return sizeNum * factor;
	}
}
