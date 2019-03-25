package com.mediatoolkit.pareco.commandline;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.mediatoolkit.pareco.util.commandline.CommandLineOptions;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 09/03/2019
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Parameters(separators = " =")
public class RunnerCommandLineOptions implements CommandLineOptions {

	@Parameter(names = {"-p", "--port"}, description = "Port to listen on for incoming http requests, port=0 means random available", order = 1)
	private int port = 0;

	@Parameter(names = {"-h", "--help"}, help = true, description = "Print this help with parameters", order = 1000)
	private boolean help;

	@Parameter(names = {"-v", "--version"}, help = true, description = "Show current build version", order = 1000)
	private boolean version;

	public void validate() {
		if (port < 0 || port > 65535) {
			throw new ParameterException("Port must be in range [1, 65535], got: " + port);
		}
	}

}
