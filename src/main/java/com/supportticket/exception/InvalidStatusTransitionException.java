package com.supportticket.exception;

import com.supportticket.entity.TicketStatus;

public class InvalidStatusTransitionException extends RuntimeException {

	public InvalidStatusTransitionException() {
		super("Invalid ticket status transition");
	}

	public InvalidStatusTransitionException(TicketStatus currentStatus, TicketStatus requestedStatus) {
		super("Ticket cannot transition from " + currentStatus + " to " + requestedStatus);
	}
}