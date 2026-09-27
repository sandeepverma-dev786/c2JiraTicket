package com.supportticket.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.supportticket.dto.ChangeTicketStatusRequest;
import com.supportticket.dto.CommentResponse;
import com.supportticket.dto.CreateCommentRequest;
import com.supportticket.dto.CreateTicketRequest;
import com.supportticket.dto.TicketResponse;
import com.supportticket.dto.UpdateTicketRequest;
import com.supportticket.entity.TicketStatus;
import com.supportticket.service.CommentService;
import com.supportticket.service.TicketService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

	private final TicketService ticketService;
	private final CommentService commentService;

	public TicketController(TicketService ticketService, CommentService commentService) {
		this.ticketService = ticketService;
		this.commentService = commentService;
	}

	@PostMapping
	public ResponseEntity<TicketResponse> createTicket(@RequestBody CreateTicketRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(request));
	}

	@GetMapping
	public List<TicketResponse> listTickets(
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "status", required = false) TicketStatus status) {
		return ticketService.listTickets(keyword, status);
	}

	@GetMapping("/{id}")
	public TicketResponse getTicketById(@PathVariable("id") UUID id) {
		return ticketService.getTicketById(id);
	}

	@PutMapping("/{id}")
	public TicketResponse updateTicket(@PathVariable("id") UUID id,
			@RequestBody UpdateTicketRequest request) {
		return ticketService.updateTicket(id, request);
	}

	@PatchMapping("/{id}/status")
	public TicketResponse changeTicketStatus(@PathVariable("id") UUID id,
			@Valid @RequestBody ChangeTicketStatusRequest request) {
		return ticketService.changeTicketStatus(id, request.status());
	}

	@PostMapping("/{id}/comments")
	public ResponseEntity<CommentResponse> addComment(@PathVariable("id") UUID id,
			@RequestBody CreateCommentRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(commentService.addComment(id, request));
	}
}