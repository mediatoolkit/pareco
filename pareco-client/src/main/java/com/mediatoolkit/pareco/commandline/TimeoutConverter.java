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
public class TimeoutConverter extends BaseConverter<Integer> implements IStringConverter<Integer> {

	private final Pattern pattern = Pattern.compile("([0-9]+)(ms|s|sec|m|min|h|hrs)");
	private final Map<String, Integer> factors = EntryStream
		.of(
			"ms", 1,
			"s", 1000,
			"sec", 1000,
			"m", 60_000,
			"min", 60_000,
			"h", 3600_000,
			"hrs", 3600_000
		)
		.toMap();

	public TimeoutConverter(String optionName) {
		super(optionName);
	}

	@Override
	public Integer convert(String value) {
		Matcher matcher = pattern.matcher(value);
		if (!matcher.matches()) {
			throw new ParameterException(getErrorString(value, "timeout duration: " + pattern));
		}
		int sizeNum = Integer.parseInt(matcher.group(1));
		int factor = factors.get(matcher.group(2));
		return sizeNum * factor;
	}
}
