package com.supportticket.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import com.supportticket.dto.ApiErrorResponse;
import com.supportticket.dto.ChangeTicketStatusRequest;
import com.supportticket.dto.CreateTicketRequest;
import com.supportticket.dto.TicketResponse;
import com.supportticket.dto.UpdateTicketRequest;
import com.supportticket.entity.Priority;
import com.supportticket.entity.TicketStatus;
import com.supportticket.repository.CommentRepository;
import com.supportticket.repository.TicketRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"spring.datasource.url=jdbc:h2:mem:ticket-api-test;DB_CLOSE_DELAY=-1",
		"spring.jpa.hibernate.ddl-auto=create-drop" })
class TicketControllerIntegrationTest {

	private static final Set<TicketStatus> OPEN_TRANSITIONS = Set.of(
			TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED);
	private static final Set<TicketStatus> IN_PROGRESS_TRANSITIONS = Set.of(
			TicketStatus.RESOLVED, TicketStatus.CANCELLED);
	private static final Set<TicketStatus> RESOLVED_TRANSITIONS = Set.of(TicketStatus.CLOSED);

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private CommentRepository commentRepository;

	@BeforeEach
	void clearDatabase() {
		commentRepository.deleteAll();
		ticketRepository.deleteAll();
	}

	@Test
	void createsTicketWithOpenStatusAndServerGeneratedFields() {
		ResponseEntity<TicketResponse> response = restTemplate.postForEntity(ticketsUrl(),
				new CreateTicketRequest("Cannot access account", "The sign-in page fails", Priority.HIGH, "Support"),
				TicketResponse.class);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertNotNull(response.getBody());
		assertNotNull(response.getBody().id());
		assertEquals(TicketStatus.OPEN, response.getBody().status());
		assertEquals("Cannot access account", response.getBody().title());
		assertEquals(Priority.HIGH, response.getBody().priority());
		assertNotNull(response.getBody().createdAt());
		assertNotNull(response.getBody().updatedAt());
	}

	@Test
	void returnsStructuredValidationErrorsForInvalidCreateFields() {
		assertValidationError("{\"title\":\"\",\"description\":\"Description\",\"priority\":\"LOW\"}", "title");
		assertValidationError("{\"title\":\"Title\",\"description\":\"\",\"priority\":\"LOW\"}", "description");
		assertValidationError("{\"title\":\"Title\",\"description\":\"Description\",\"priority\":null}", "priority");
		assertValidationError("{\"title\":\"Title\",\"description\":\"Description\","
				+ "\"priority\":\"NOT_A_PRIORITY\"}", "priority");
		assertValidationError("{\"title\":\"" + "x".repeat(201)
				+ "\",\"description\":\"Description\",\"priority\":\"LOW\"}", "title");
		assertValidationError("{\"title\":\"Title\",\"description\":\"" + "x".repeat(2001)
				+ "\",\"priority\":\"LOW\"}", "description");
	}

	@Test
	void getsExistingTicketAndReturnsStructuredNotFoundForMissingTicket() {
		UUID ticketId = createTicket("Details title", "Details description", Priority.MEDIUM);

		ResponseEntity<TicketResponse> found = restTemplate.getForEntity(ticketUrl(ticketId), TicketResponse.class);
		assertEquals(HttpStatus.OK, found.getStatusCode());
		assertNotNull(found.getBody());
		assertEquals(ticketId, found.getBody().id());
		assertEquals("Details title", found.getBody().title());
		assertNotNull(found.getBody().comments());

		UUID missingId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		ResponseEntity<ApiErrorResponse> missing = restTemplate.getForEntity(ticketUrl(missingId), ApiErrorResponse.class);
		assertErrorResponse(missing, HttpStatus.NOT_FOUND, "NOT_FOUND", ticketPath(missingId));
	}

	@Test
	void updatesEditableFieldsWithoutChangingStatusAndRejectsStatusInPut() {
		UUID ticketId = createTicket("Original title", "Original description", Priority.LOW);
		ResponseEntity<TicketResponse> updated = restTemplate.exchange(ticketUrl(ticketId), HttpMethod.PUT,
				new HttpEntity<>(new UpdateTicketRequest("Changed title", "Changed description", Priority.CRITICAL,
						"Account Support")), TicketResponse.class);

		assertEquals(HttpStatus.OK, updated.getStatusCode());
		assertNotNull(updated.getBody());
		assertEquals("Changed title", updated.getBody().title());
		assertEquals("Changed description", updated.getBody().description());
		assertEquals(Priority.CRITICAL, updated.getBody().priority());
		assertEquals("Account Support", updated.getBody().assignee());
		assertEquals(TicketStatus.OPEN, updated.getBody().status());

		ResponseEntity<ApiErrorResponse> forbiddenStatus = putJson(ticketId,
				"{\"title\":\"Changed title\",\"description\":\"Changed description\","
						+ "\"priority\":\"CRITICAL\",\"status\":\"CLOSED\"}");
		assertErrorResponse(forbiddenStatus, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ticketPath(ticketId));
		assertEquals(TicketStatus.OPEN, getTicket(ticketId).status());
	}

	@Test
	void returnsStructuredValidationErrorsForInvalidUpdate() {
		UUID ticketId = createTicket("Original title", "Original description", Priority.LOW);

		ResponseEntity<ApiErrorResponse> response = putJson(ticketId,
				"{\"title\":\"\",\"description\":\"Description\",\"priority\":\"LOW\"}");

		assertErrorResponse(response, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ticketPath(ticketId));
		assertTrue(response.getBody().errors().containsKey("title"));
	}

	@Test
	void searchesBothFieldsCaseInsensitivelyAndCombinesWithStatus() {
		UUID titleMatchId = createTicket("Login failure", "Account page is unavailable", Priority.HIGH);
		UUID descriptionMatchId = createTicket("Network issue", "LOGIN service unavailable", Priority.MEDIUM);
		UUID unrelatedId = createTicket("Printer issue", "Paper jam", Priority.LOW);
		changeStatus(descriptionMatchId, TicketStatus.IN_PROGRESS);

		TicketResponse[] keywordMatches = getTickets("?keyword=LOGIN");
		assertEquals(2, keywordMatches.length);
		assertTrue(containsTicket(keywordMatches, titleMatchId));
		assertTrue(containsTicket(keywordMatches, descriptionMatchId));
		assertFalse(containsTicket(keywordMatches, unrelatedId));

		TicketResponse[] statusMatches = getTickets("?status=IN_PROGRESS");
		assertEquals(1, statusMatches.length);
		assertEquals(descriptionMatchId, statusMatches[0].id());

		TicketResponse[] combinedMatches = getTickets("?keyword=login&status=IN_PROGRESS");
		assertEquals(1, combinedMatches.length);
		assertEquals(descriptionMatchId, combinedMatches[0].id());
	}

	@Test
	void blankKeywordDoesNotFilterTickets() {
		createTicket("Alpha", "First description", Priority.LOW);
		createTicket("Beta", "Second description", Priority.MEDIUM);
		createTicket("Gamma", "Third description", Priority.HIGH);

		URI blankKeywordUri = UriComponentsBuilder.fromUriString(ticketsUrl())
				.queryParam("keyword", "  \t")
				.build()
				.encode()
				.toUri();
		TicketResponse[] tickets = restTemplate.getForObject(blankKeywordUri, TicketResponse[].class);

		assertEquals(3, tickets.length);
	}

	@Test
	void appliesEveryValidStatusTransitionThroughRestAndPersistsIt() {
		List<TransitionCase> transitions = List.of(
				new TransitionCase(List.of(), TicketStatus.IN_PROGRESS),
				new TransitionCase(List.of(TicketStatus.IN_PROGRESS), TicketStatus.RESOLVED),
				new TransitionCase(List.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED), TicketStatus.CLOSED),
				new TransitionCase(List.of(), TicketStatus.CANCELLED),
				new TransitionCase(List.of(TicketStatus.IN_PROGRESS), TicketStatus.CANCELLED));

		for (TransitionCase transition : transitions) {
			UUID ticketId = createTicket("Transition ticket", "State machine test", Priority.MEDIUM);
			for (TicketStatus setupStatus : transition.setupTransitions()) {
				assertEquals(HttpStatus.OK, patchStatus(ticketId, setupStatus).getStatusCode());
			}

			ResponseEntity<TicketResponse> changed = patchStatus(ticketId, transition.targetStatus());
			assertEquals(HttpStatus.OK, changed.getStatusCode());
			assertNotNull(changed.getBody());
			assertEquals(transition.targetStatus(), changed.getBody().status());
			assertEquals(transition.targetStatus(), getTicket(ticketId).status());
		}
	}

	@Test
	void rejectsEveryInvalidTransitionAndKeepsPersistedStatusUnchanged() {
		for (TicketStatus currentStatus : TicketStatus.values()) {
			UUID ticketId = createTicket("Invalid transition ticket", "State machine test", Priority.MEDIUM);
			for (TicketStatus setupStatus : pathTo(currentStatus)) {
				assertEquals(HttpStatus.OK, patchStatus(ticketId, setupStatus).getStatusCode());
			}

			for (TicketStatus requestedStatus : TicketStatus.values()) {
				if (!allowedTransitionsFrom(currentStatus).contains(requestedStatus)) {
					ResponseEntity<ApiErrorResponse> rejected = patchStatusForError(ticketId, requestedStatus);
					assertErrorResponse(rejected, HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION",
							ticketPath(ticketId) + "/status");
					assertTrue(rejected.getBody().message().contains(currentStatus.name()));
					assertTrue(rejected.getBody().message().contains(requestedStatus.name()));
					assertEquals(currentStatus, getTicket(ticketId).status());
				}
			}
		}
	}

	private void assertValidationError(String requestBody, String field) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(ticketsUrl(), HttpMethod.POST,
				new HttpEntity<>(requestBody, headers), ApiErrorResponse.class);
		assertErrorResponse(response, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "/api/tickets");
		assertTrue(response.getBody().errors().containsKey(field));
	}

	private ResponseEntity<ApiErrorResponse> putJson(UUID ticketId, String requestBody) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return restTemplate.exchange(ticketUrl(ticketId), HttpMethod.PUT, new HttpEntity<>(requestBody, headers),
				ApiErrorResponse.class);
	}

	private ResponseEntity<TicketResponse> patchStatus(UUID ticketId, TicketStatus status) {
		return restTemplate.exchange(statusUrl(ticketId), HttpMethod.PATCH,
				new HttpEntity<>(new ChangeTicketStatusRequest(status)), TicketResponse.class);
	}

	private ResponseEntity<ApiErrorResponse> patchStatusForError(UUID ticketId, TicketStatus status) {
		return restTemplate.exchange(statusUrl(ticketId), HttpMethod.PATCH,
				new HttpEntity<>(new ChangeTicketStatusRequest(status)), ApiErrorResponse.class);
	}

	private UUID createTicket(String title, String description, Priority priority) {
		ResponseEntity<TicketResponse> response = restTemplate.postForEntity(ticketsUrl(),
				new CreateTicketRequest(title, description, priority, null), TicketResponse.class);
		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		return response.getBody().id();
	}

	private void changeStatus(UUID ticketId, TicketStatus status) {
		assertEquals(HttpStatus.OK, patchStatus(ticketId, status).getStatusCode());
	}

	private TicketResponse getTicket(UUID ticketId) {
		return restTemplate.getForObject(ticketUrl(ticketId), TicketResponse.class);
	}

	private TicketResponse[] getTickets(String query) {
		return restTemplate.getForObject(ticketsUrl() + query, TicketResponse[].class);
	}

	private boolean containsTicket(TicketResponse[] tickets, UUID ticketId) {
		return Arrays.stream(tickets).anyMatch(ticket -> ticket.id().equals(ticketId));
	}

	private void assertErrorResponse(ResponseEntity<ApiErrorResponse> response, HttpStatus status,
			String code, String path) {
		assertEquals(status, response.getStatusCode());
		assertNotNull(response.getBody());
		assertNotNull(response.getBody().timestamp());
		assertEquals(status.value(), response.getBody().status());
		assertEquals(code, response.getBody().code());
		assertFalse(response.getBody().message().isBlank());
		assertEquals(path, response.getBody().path());
	}

	private String ticketsUrl() {
		return "http://localhost:" + port + "/api/tickets";
	}

	private String ticketUrl(UUID ticketId) {
		return ticketPath(ticketId);
	}

	private String statusUrl(UUID ticketId) {
		return ticketUrl(ticketId) + "/status";
	}

	private String ticketPath(UUID ticketId) {
		return "/api/tickets/" + ticketId;
	}

	private List<TicketStatus> pathTo(TicketStatus status) {
		return switch (status) {
			case OPEN -> List.of();
			case IN_PROGRESS -> List.of(TicketStatus.IN_PROGRESS);
			case RESOLVED -> List.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED);
			case CLOSED -> List.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED, TicketStatus.CLOSED);
			case CANCELLED -> List.of(TicketStatus.CANCELLED);
		};
	}

	private Set<TicketStatus> allowedTransitionsFrom(TicketStatus status) {
		return switch (status) {
			case OPEN -> OPEN_TRANSITIONS;
			case IN_PROGRESS -> IN_PROGRESS_TRANSITIONS;
			case RESOLVED -> RESOLVED_TRANSITIONS;
			case CLOSED, CANCELLED -> Set.of();
		};
	}

	private record TransitionCase(List<TicketStatus> setupTransitions, TicketStatus targetStatus) {
	}
}