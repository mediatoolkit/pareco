package com.mediatoolkit.pareco.restapi;

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
		if (!(ex instanceof ParecoException)) {
			log.warn("Unhandled server exception: ", ex);
		}
		ErrorBody errorBody = ErrorBody.builder()
			.error("Server Exception")
			.status(500)
			.message(ex.toString())
			.path(request.getContextPath())
			.timestamp(new Date())
			.build();
		log.error("Exception returning error: {}", errorBody);
		return new ResponseEntity<>(errorBody, HttpStatus.INTERNAL_SERVER_ERROR);
	}

}
