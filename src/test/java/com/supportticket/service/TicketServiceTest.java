package com.supportticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.supportticket.dto.CreateTicketRequest;
import com.supportticket.dto.TicketResponse;
import com.supportticket.dto.UpdateTicketRequest;
import com.supportticket.entity.Priority;
import com.supportticket.entity.Ticket;
import com.supportticket.entity.TicketStatus;
import com.supportticket.exception.InvalidStatusTransitionException;
import com.supportticket.exception.TicketNotFoundException;
import com.supportticket.repository.TicketRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

class TicketServiceTest {

	private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
	private static final Map<TicketStatus, Set<TicketStatus>> VALID_TRANSITIONS = Map.of(
			TicketStatus.OPEN, Set.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
			TicketStatus.IN_PROGRESS, Set.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED),
			TicketStatus.RESOLVED, Set.of(TicketStatus.CLOSED),
			TicketStatus.CLOSED, Set.of(),
			TicketStatus.CANCELLED, Set.of());
	private static final UUID TICKET_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

	private TicketRepository ticketRepository;
	private TicketService ticketService;

	@BeforeEach
	void setUp() {
		ticketRepository = mock(TicketRepository.class);
		ticketService = new TicketService(ticketRepository, VALIDATOR_FACTORY.getValidator());
	}

	@AfterAll
	static void closeValidatorFactory() {
		VALIDATOR_FACTORY.close();
	}

	@Test
	void createsTicketWithOpenStatusAndApprovedFields() {
		when(ticketRepository.saveAndFlush(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TicketResponse response = ticketService.createTicket(
				new CreateTicketRequest("Login issue", "Cannot sign in", Priority.HIGH, "Support"));

		verify(ticketRepository).saveAndFlush(any(Ticket.class));
		assertEquals(TicketStatus.OPEN, response.status());
		assertEquals("Login issue", response.title());
		assertEquals("Cannot sign in", response.description());
		assertEquals(Priority.HIGH, response.priority());
		assertEquals("Support", response.assignee());
	}

	@Test
	void rejectsInvalidCreateRequestBeforeSaving() {
		assertThrows(ConstraintViolationException.class, () -> ticketService.createTicket(
				new CreateTicketRequest("", "A description", null, null)));

		verify(ticketRepository, never()).saveAndFlush(any(Ticket.class));
	}

	@Test
	void getsTicketByIdOrThrowsWhenMissing() {
		Ticket ticket = ticketWithStatus(TicketStatus.OPEN);
		when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));

		assertEquals(TicketStatus.OPEN, ticketService.getTicketById(TICKET_ID).status());

		when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.empty());
		assertThrows(TicketNotFoundException.class, () -> ticketService.getTicketById(TICKET_ID));
	}

	@Test
	void delegatesKeywordAndStatusFiltersToRepository() {
		when(ticketRepository.findByFilters(" \t ", TicketStatus.OPEN)).thenReturn(java.util.List.of());

		assertEquals(java.util.List.of(), ticketService.listTickets(" \t ", TicketStatus.OPEN));

		verify(ticketRepository).findByFilters(" \t ", TicketStatus.OPEN);
	}

	@Test
	void updatesOnlyEditableTicketFields() {
		Ticket ticket = ticketWithStatus(TicketStatus.IN_PROGRESS);
		when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
		when(ticketRepository.saveAndFlush(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TicketResponse response = ticketService.updateTicket(TICKET_ID,
				new UpdateTicketRequest("Updated title", "Updated description", Priority.CRITICAL, null));

		assertEquals("Updated title", ticket.getTitle());
		assertEquals("Updated description", ticket.getDescription());
		assertEquals(Priority.CRITICAL, ticket.getPriority());
		assertNull(ticket.getAssignee());
		assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
		assertEquals(TicketStatus.IN_PROGRESS, response.status());
		verify(ticketRepository).saveAndFlush(ticket);
	}

	@Test
	void updateOfMissingTicketThrowsNotFoundWithoutSaving() {
		when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.empty());

		assertThrows(TicketNotFoundException.class, () -> ticketService.updateTicket(TICKET_ID,
				new UpdateTicketRequest("Title", "Description", Priority.LOW, null)));

		verify(ticketRepository, never()).saveAndFlush(any(Ticket.class));
	}

	@ParameterizedTest
	@MethodSource("validTransitions")
	void appliesEveryAllowedStatusTransition(TicketStatus currentStatus, TicketStatus requestedStatus) {
		Ticket ticket = ticketWithStatus(currentStatus);
		when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
		when(ticketRepository.saveAndFlush(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

		assertEquals(requestedStatus, ticketService.changeTicketStatus(TICKET_ID, requestedStatus).status());
		verify(ticketRepository).saveAndFlush(ticket);
	}

	@ParameterizedTest
	@MethodSource("invalidTransitions")
	void rejectsEveryOtherTransitionWithoutChangingOrSaving(TicketStatus currentStatus,
			TicketStatus requestedStatus) {
		Ticket ticket = ticketWithStatus(currentStatus);
		when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));

		InvalidStatusTransitionException exception = assertThrows(InvalidStatusTransitionException.class,
				() -> ticketService.changeTicketStatus(TICKET_ID, requestedStatus));

		assertEquals(currentStatus, ticket.getStatus());
		assertEquals("Ticket cannot transition from " + currentStatus + " to " + requestedStatus,
				exception.getMessage());
		verify(ticketRepository, never()).save(any(Ticket.class));
	}

	private static Stream<Arguments> validTransitions() {
		return Stream.of(
				Arguments.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS),
				Arguments.of(TicketStatus.OPEN, TicketStatus.CANCELLED),
				Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED),
				Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
				Arguments.of(TicketStatus.RESOLVED, TicketStatus.CLOSED));
	}

	private static Stream<Arguments> invalidTransitions() {
		return Arrays.stream(TicketStatus.values())
				.flatMap(currentStatus -> Arrays.stream(TicketStatus.values())
						.filter(requestedStatus -> !VALID_TRANSITIONS.get(currentStatus).contains(requestedStatus))
						.map(requestedStatus -> Arguments.of(currentStatus, requestedStatus)));
	}

	private Ticket ticketWithStatus(TicketStatus status) {
		Ticket ticket = new Ticket("Original title", "Original description", Priority.MEDIUM, "Original assignee");
		ticket.setStatus(status);
		return ticket;
	}
}