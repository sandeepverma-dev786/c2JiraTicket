package com.supportticket.exception;

public class TicketNotFoundException extends RuntimeException {

	public TicketNotFoundException() {
		super("Ticket not found");
	}
}