package com.supportticket.exception;

public class InvalidStatusTransitionException extends RuntimeException {

	public InvalidStatusTransitionException() {
		super("Invalid ticket status transition");
	}
}