package com.mediatoolkit.pareco.model;

import java.util.Date;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 30/10/2018
 */
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
@Builder
public class ErrorBody {

	private Date timestamp;
	private int status;
	private String error;
	private String message;
	private String path;
	private Type type;

	public enum Type {
		FILE_DELETED,
		ILLEGAL_STATE,
		UNKNOWN
	}
}
