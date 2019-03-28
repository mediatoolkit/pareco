package com.mediatoolkit.pareco.util.commandline;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 2019-03-25
 */
public interface CommandLineOptions {

	boolean isVersion();

	boolean isHelp();

	void validate();
}
