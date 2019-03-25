package com.mediatoolkit.pareco.restapi;

import com.mediatoolkit.pareco.auth.Authenticator;
import com.mediatoolkit.pareco.components.DirectoryStructureReader;
import com.mediatoolkit.pareco.components.TransferNamesEncoding;
import com.mediatoolkit.pareco.model.DirectoryStructure;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 2019-03-23
 */
@RequestMapping("/list")
@AllArgsConstructor(onConstructor = @__(@Autowired))
@RestController
public class ListApi {

	private final TransferNamesEncoding encoding;
	private final Authenticator authenticator;
	private final DirectoryStructureReader directoryStructureReader;

	private String decode(String val) {
		return encoding.decode(val);
	}

	@GetMapping("/dir")
	public DirectoryStructure getDirectoryStructure(
		@RequestParam("serverRootDirectory") String serverRootDirectory,
		@RequestParam(name = "authToken", required = false) String authToken,
		@RequestParam(name = "include", required = false) String include,
		@RequestParam(name = "exclude", required = false) String exclude
	) throws IOException {
		authenticator.authenticate(authToken);
		return directoryStructureReader.readDirectoryStructure(
			decode(serverRootDirectory), decode(include), decode(exclude)
		);
	}
}
