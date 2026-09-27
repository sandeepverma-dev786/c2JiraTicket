package com.supportticket.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.supportticket.dto.CommentResponse;
import com.supportticket.dto.CreateTicketRequest;
import com.supportticket.dto.TicketResponse;
import com.supportticket.dto.UpdateTicketRequest;
import com.supportticket.entity.Comment;
import com.supportticket.entity.Ticket;
import com.supportticket.entity.TicketStatus;
import com.supportticket.exception.InvalidStatusTransitionException;
import com.supportticket.exception.TicketNotFoundException;
import com.supportticket.repository.TicketRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@Service
public class TicketService {

	private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED_TRANSITIONS = Map.of(
			TicketStatus.OPEN, Set.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
			TicketStatus.IN_PROGRESS, Set.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED),
			TicketStatus.RESOLVED, Set.of(TicketStatus.CLOSED),
			TicketStatus.CLOSED, Set.of(),
			TicketStatus.CANCELLED, Set.of());

	private final TicketRepository ticketRepository;
	private final Validator validator;

	public TicketService(TicketRepository ticketRepository, Validator validator) {
		this.ticketRepository = ticketRepository;
		this.validator = validator;
	}

	@Transactional
	public TicketResponse createTicket(CreateTicketRequest request) {
		validate(request);
		Ticket ticket = new Ticket(request.title(), request.description(), request.priority(), request.assignee());
		ticket.setStatus(TicketStatus.OPEN);
		return toResponse(ticketRepository.saveAndFlush(ticket));
	}

	@Transactional(readOnly = true)
	public TicketResponse getTicketById(UUID ticketId) {
		return ticketRepository.findById(ticketId)
				.map(this::toDetailResponse)
				.orElseThrow(TicketNotFoundException::new);
	}

	@Transactional(readOnly = true)
	public List<TicketResponse> listTickets(String keyword, TicketStatus status) {
		return ticketRepository.findByFilters(keyword, status).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public TicketResponse updateTicket(UUID ticketId, UpdateTicketRequest request) {
		validate(request);
		Ticket ticket = findTicket(ticketId);
		ticket.setTitle(request.title());
		ticket.setDescription(request.description());
		ticket.setPriority(request.priority());
		ticket.setAssignee(request.assignee());
		return toResponse(ticketRepository.saveAndFlush(ticket));
	}

	@Transactional
	public TicketResponse changeTicketStatus(UUID ticketId, TicketStatus requestedStatus) {
		Ticket ticket = findTicket(ticketId);
		TicketStatus currentStatus = ticket.getStatus();
		Set<TicketStatus> allowedStatuses = ALLOWED_TRANSITIONS.get(currentStatus);
		if (requestedStatus == null || allowedStatuses == null || !allowedStatuses.contains(requestedStatus)) {
			throw new InvalidStatusTransitionException(currentStatus, requestedStatus);
		}

		ticket.setStatus(requestedStatus);
		return toResponse(ticketRepository.saveAndFlush(ticket));
	}

	private Ticket findTicket(UUID ticketId) {
		return ticketRepository.findById(ticketId).orElseThrow(TicketNotFoundException::new);
	}

	private <T> void validate(T request) {
		var violations = validator.validate(request);
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
	}

	private TicketResponse toResponse(Ticket ticket) {
		return toResponse(ticket, null);
	}

	private TicketResponse toDetailResponse(Ticket ticket) {
		List<CommentResponse> comments = ticket.getComments().stream()
				.map(this::toCommentResponse)
				.toList();
		return toResponse(ticket, comments);
	}

	private CommentResponse toCommentResponse(Comment comment) {
		return new CommentResponse(
				comment.getId(),
				comment.getTicket().getId(),
				comment.getAuthor(),
				comment.getContent(),
				comment.getCreatedAt());
	}

	private TicketResponse toResponse(Ticket ticket, List<CommentResponse> comments) {
		return new TicketResponse(
				ticket.getId(),
				ticket.getTitle(),
				ticket.getDescription(),
				ticket.getPriority(),
				ticket.getStatus(),
				ticket.getAssignee(),
				ticket.getCreatedAt(),
				ticket.getUpdatedAt(),
				comments);
	}
}