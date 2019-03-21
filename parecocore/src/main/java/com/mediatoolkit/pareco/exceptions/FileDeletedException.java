package com.mediatoolkit.pareco.exceptions;

import com.mediatoolkit.pareco.model.ErrorBody.Type;
import com.mediatoolkit.pareco.model.FilePath;
import lombok.Getter;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 20/03/2019
 */
public class FileDeletedException extends ParecoException {

	@Getter
	private final FilePath filePath;

	public FileDeletedException(FilePath filePath, String message, Throwable cause) {
		super(message, cause);
		this.filePath = filePath;
	}

	@Override
	public Type type() {
		return Type.FILE_DELETED;
	}
}
