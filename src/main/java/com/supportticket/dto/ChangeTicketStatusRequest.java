package com.supportticket.dto;

import com.supportticket.entity.TicketStatus;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public record ChangeTicketStatusRequest(
		@NotNull(message = "Status is required")
		TicketStatus status) {

	@JsonAnySetter
	public void rejectUnknownProperty(String name, JsonNode value) {
		throw new IllegalArgumentException("Unrecognized request field");
	}
}