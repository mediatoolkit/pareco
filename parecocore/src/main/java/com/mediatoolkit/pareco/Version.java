package com.mediatoolkit.pareco;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 06/11/2018
 */
public class Version {

	private static String version;

	static {
		InputStream versionStream = Version.class.getClassLoader().getResourceAsStream("version.txt");
		if (versionStream == null) {
			version = null;
		}
		try {
			version = IOUtils.resourceToString("version.txt", Charset.forName("utf-8"), Version.class.getClassLoader());
		} catch (IOException e) {
			version = null;
		}
	}

	public static String getVersion() {
		return version;
	}

}
