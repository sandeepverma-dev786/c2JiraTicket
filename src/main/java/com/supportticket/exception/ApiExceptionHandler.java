package com.supportticket.exception;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.supportticket.dto.ApiErrorResponse;

import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(BindException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(BindException exception,
			HttpServletRequest request) {
		Map<String, String> errors = new TreeMap<>();
		exception.getBindingResult().getFieldErrors().forEach(error ->
				errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		exception.getBindingResult().getGlobalErrors().forEach(error ->
				errors.putIfAbsent("request", error.getDefaultMessage()));
		return errorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed",
				errors, request);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception,
			HttpServletRequest request) {
		Map<String, String> errors = new TreeMap<>();
		exception.getConstraintViolations().forEach(violation -> {
			String propertyPath = violation.getPropertyPath().toString();
			String field = propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
			errors.putIfAbsent(field, violation.getMessage());
		});
		return errorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed",
				errors, request);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception,
			HttpServletRequest request) {
		Map<String, String> errors = Map.of(exception.getName(), "Invalid value");
		return errorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed",
				errors, request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException exception,
			HttpServletRequest request) {
		String field = findInvalidField(exception);
		Map<String, String> errors = Map.of(field, field.equals("request")
				? "Malformed request body" : "Invalid value");
		return errorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed",
				errors, request);
	}

	@ExceptionHandler(TicketNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleTicketNotFound(TicketNotFoundException exception,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ticket not found", null, request);
	}

	@ExceptionHandler(InvalidStatusTransitionException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidStatusTransition(
			InvalidStatusTransitionException exception, HttpServletRequest request) {
		return errorResponse(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION",
				exception.getMessage(), null, request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
		logger.error("Unexpected error processing {} {}", request.getMethod(), request.getRequestURI(), exception);
		return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
				"An unexpected error occurred", null, request);
	}

	private ResponseEntity<ApiErrorResponse> errorResponse(HttpStatus status, String code, String message,
			Map<String, String> errors, HttpServletRequest request) {
		ApiErrorResponse response = new ApiErrorResponse(Instant.now(), status.value(), code, message, errors,
				request.getRequestURI());
		return ResponseEntity.status(status).body(response);
	}

	private String findInvalidField(HttpMessageNotReadableException exception) {
		Throwable cause = exception;
		while (cause != null) {
			if (cause instanceof JsonMappingException mappingException) {
				String field = mappingException.getPath().stream()
						.map(JsonMappingException.Reference::getFieldName)
						.filter(Objects::nonNull)
						.reduce((first, last) -> last)
						.orElse("request");
				if (!field.equals("request")) {
					return field;
				}
			}
			cause = cause.getCause();
		}
		return "request";
	}
}