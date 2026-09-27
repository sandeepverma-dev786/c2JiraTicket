package com.supportticket.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
		@NotBlank(message = "Author is required")
		String author,
		@NotBlank(message = "Content is required")
		@Size(min = 1, max = 1000, message = "Content must contain 1-1000 characters")
		String content) {

	@JsonAnySetter
	public void rejectUnknownProperty(String name, JsonNode value) {
		throw new IllegalArgumentException("Unrecognized request field");
	}
}