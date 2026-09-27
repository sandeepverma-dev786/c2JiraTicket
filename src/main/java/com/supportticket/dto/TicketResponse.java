package com.supportticket.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supportticket.entity.Priority;
import com.supportticket.entity.TicketStatus;

public record TicketResponse(
		UUID id,
		String title,
		String description,
		Priority priority,
		TicketStatus status,
		String assignee,
		Instant createdAt,
		Instant updatedAt,
		@JsonInclude(JsonInclude.Include.NON_NULL) List<CommentResponse> comments) {
}