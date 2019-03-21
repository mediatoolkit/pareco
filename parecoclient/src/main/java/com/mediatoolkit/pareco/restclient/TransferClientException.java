package com.mediatoolkit.pareco.restclient;

import com.mediatoolkit.pareco.exceptions.ParecoException;
import com.mediatoolkit.pareco.model.ErrorBody;
import com.mediatoolkit.pareco.model.ErrorBody.Type;
import lombok.Getter;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 30/10/2018
 */
public abstract class TransferClientException extends ParecoException {

	TransferClientException(String message) {
		super(message);
	}

	public static class ServerSideTransferClientException extends TransferClientException {

		@Getter
		private final ErrorBody errorBody;

		public ServerSideTransferClientException(ErrorBody errorBody) {
			super(String.format("%d: %s: %s", errorBody.getStatus(), errorBody.getError(), errorBody.getMessage()));
			this.errorBody = errorBody;
		}

		@Override
		public Type type() {
			return errorBody.getType();
		}
		
		public static class FileDeletedOnServerSideException extends ServerSideTransferClientException {

			public FileDeletedOnServerSideException(ErrorBody errorBody) {
				super(errorBody);
			}
		}

	}

	public static class UnknownErrorTransferClientException extends TransferClientException {

		public UnknownErrorTransferClientException(String message) {
			super(message);
		}

		@Override
		public Type type() {
			return Type.UNKNOWN;
		}
	}
}
