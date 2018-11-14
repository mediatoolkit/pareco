package com.mediatoolkit.pareco.model;

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
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class FileEntry {

	private FilePath filePath;
	private boolean directory;

	public static FileEntry file(FilePath filePath) {
		return new FileEntry(filePath, false);
	}

	public static FileEntry directory(FilePath filePath) {
		return new FileEntry(filePath, true);
	}

}
