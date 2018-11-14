package com.mediatoolkit.pareco.model;

import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 23/10/2018
 */
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
@Builder
public class FileMetadata {

	private FilePath filePath;
	private long fileSizeBytes;
	private long lastModifiedTimeMillis;
	/**
	 * Can be {@code null} of filesystems that do not support posix
	 */
	private Set<PosixFilePermission> permissions;

}
