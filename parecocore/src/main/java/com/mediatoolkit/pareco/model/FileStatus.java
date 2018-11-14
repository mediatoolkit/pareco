package com.mediatoolkit.pareco.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 24/10/2018
 */
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class FileStatus {

	private boolean exist;
	private FileMetadata fileMetadata;

	public static final FileStatus NOT_EXIST = new FileStatus(false, null);

	public static FileStatus of(FileMetadata fileMetadata) {
		return new FileStatus(true, fileMetadata);
	}
}
