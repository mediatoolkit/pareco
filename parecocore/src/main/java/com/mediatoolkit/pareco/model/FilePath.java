package com.mediatoolkit.pareco.model;

import java.io.File;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 25/10/2018
 */
@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class FilePath {

	private String relativeDirectory;
	private String fileName;

	public static FilePath of(String fileName) {
		return of("", fileName);
	}

	public String toRelativePath() {
		if (relativeDirectory.isEmpty()) {
			return fileName;
		} else {
			return relativeDirectory + File.separator + fileName;
		}
	}

	public String toAbsolutePath(String rootDirectory) {
		return rootDirectory + File.separator + toRelativePath();
	}

	@Override
	public String toString() {
		return toRelativePath();
	}

}
