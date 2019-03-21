package com.mediatoolkit.pareco.restapi;

import com.mediatoolkit.pareco.exceptions.FileDeletedException;
import com.mediatoolkit.pareco.exceptions.ParecoException;
import com.mediatoolkit.pareco.model.ErrorBody;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 05/11/2018
 */
@Slf4j
@RestControllerAdvice
public class TransferExceptionHandler extends ResponseEntityExceptionHandler {

	@Order(Ordered.HIGHEST_PRECEDENCE)
	@ExceptionHandler(Exception.class)
	public final ResponseEntity<ErrorBody> handleParecoException(Exception ex, WebRequest request) {
		ErrorBody.Type type;
		String message;
		if (ex instanceof ParecoException) {
			type = ((ParecoException) ex).type();
			if (ex instanceof FileDeletedException) {
				message = "File deleted concurrently: " + ((FileDeletedException) ex).getFilePath().toRelativePath();
			} else {
				message = ex.toString();
			}
		} else {
			log.warn("Unhandled server exception: ", ex);
			type = ErrorBody.Type.UNKNOWN;
			message = ex.toString();
		}
		ErrorBody errorBody = ErrorBody.builder()
			.error("Server Exception")
			.status(500)
			.message(message)
			.path(request.getContextPath())
			.timestamp(new Date())
			.type(type)
			.build();
		switch (type) {
			case FILE_DELETED:
				log.info(message);
				break;
			case ILLEGAL_STATE:
				log.warn("Illegal state: {}", errorBody);
				break;
			default:
				log.error("Exception returning error: {}", errorBody);
		}
		return new ResponseEntity<>(errorBody, HttpStatus.INTERNAL_SERVER_ERROR);
	}

}
