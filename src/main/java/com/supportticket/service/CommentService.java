package com.supportticket.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.supportticket.dto.CommentResponse;
import com.supportticket.dto.CreateCommentRequest;
import com.supportticket.entity.Comment;
import com.supportticket.entity.Ticket;
import com.supportticket.exception.TicketNotFoundException;
import com.supportticket.repository.CommentRepository;
import com.supportticket.repository.TicketRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@Service
public class CommentService {

	private final CommentRepository commentRepository;
	private final TicketRepository ticketRepository;
	private final Validator validator;

	public CommentService(CommentRepository commentRepository, TicketRepository ticketRepository,
			Validator validator) {
		this.commentRepository = commentRepository;
		this.ticketRepository = ticketRepository;
		this.validator = validator;
	}

	@Transactional
	public CommentResponse addComment(UUID ticketId, CreateCommentRequest request) {
		validate(request);
		Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(TicketNotFoundException::new);
		Comment comment = commentRepository.saveAndFlush(new Comment(ticket, request.author(), request.content()));
		return new CommentResponse(
				comment.getId(),
				comment.getTicket().getId(),
				comment.getAuthor(),
				comment.getContent(),
				comment.getCreatedAt());
	}

	private void validate(CreateCommentRequest request) {
		var violations = validator.validate(request);
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
	}
}