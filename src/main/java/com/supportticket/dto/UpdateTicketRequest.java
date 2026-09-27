package com.supportticket.dto;

import com.supportticket.entity.Priority;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
		@NotBlank(message = "Title is required")
		@Size(min = 1, max = 200, message = "Title must contain 1-200 characters")
		String title,
		@NotBlank(message = "Description is required")
		@Size(min = 1, max = 2000, message = "Description must contain 1-2000 characters")
		String description,
		@NotNull(message = "Priority is required")
		Priority priority,
		String assignee) {

	@JsonAnySetter
	public void rejectUnknownProperty(String name, JsonNode value) {
		throw new IllegalArgumentException("Unrecognized request field");
	}
}