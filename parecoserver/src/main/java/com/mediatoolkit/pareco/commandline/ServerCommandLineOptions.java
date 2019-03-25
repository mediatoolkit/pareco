package com.mediatoolkit.pareco.commandline;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 03/11/2018
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Parameters(separators = " =")
public class ServerCommandLineOptions {

	@Parameter(names = {"-p", "--port"}, description = "Port to listen on for incoming http requests, port=0 means random available", order = 1)
	private int port = 0;

	@Parameter(names = {"-a", "--authToken"}, description = "Authentication token to use to authorize clients", order = 2)
	private String authToken;

	@Parameter(names = {"-g", "--generateAuth"}, description = "If set, authToken for authentication will be auto-generated and printed", order = 3)
	private boolean generateToken;

	@Parameter(names = {"-e", "--expire"}, description = "Duration in millis after inactive sessions get expired", order = 4)
	private int sessionExpire = 150_000;

	@Parameter(names = {"-h", "--help"}, help = true, description = "Print this help with parameters", order = 1000)
	private boolean help;

	@Parameter(names = {"-v", "--version"}, help = true, description = "Show current build version", order = 1000)
	private boolean version;

	public void validate() {
		if (generateToken && authToken != null) {
			throw new ParameterException("When generate token option is set, no auth token should be specified");
		}
		if (authToken != null && authToken.isEmpty()) {
			throw new ParameterException("Auth token must not be empty string");
		}
		if (port < 0 || port > 65535) {
			throw new ParameterException("Port must be in range [1, 65535], got: " + port);
		}
	}

}
