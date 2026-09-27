package com.supportticket.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Instant;
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

import com.supportticket.dto.ApiErrorResponse;
import com.supportticket.dto.CommentResponse;
import com.supportticket.dto.CreateCommentRequest;
import com.supportticket.dto.CreateTicketRequest;
import com.supportticket.dto.TicketResponse;
import com.supportticket.entity.Priority;
import com.supportticket.repository.CommentRepository;
import com.supportticket.repository.TicketRepository;
import com.supportticket.service.TicketService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"spring.datasource.url=jdbc:h2:mem:comment-api-test;DB_CLOSE_DELAY=-1",
		"spring.jpa.hibernate.ddl-auto=create-drop" })
class CommentControllerIntegrationTest {

	private static final UUID CLIENT_COMMENT_ID = UUID.fromString("7b8e6f40-2e9a-4d1f-bb94-4a1e9a8bb4f2");
	private static final Instant CLIENT_TIMESTAMP = Instant.parse("2020-01-01T00:00:00Z");

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private TicketService ticketService;

	@BeforeEach
	void clearDatabase() {
		commentRepository.deleteAll();
		ticketRepository.deleteAll();
	}

	@Test
	void createsCommentWithSuppliedFieldsAndServerGeneratedValues() {
		UUID ticketId = createTicket();

		ResponseEntity<CommentResponse> response = restTemplate.postForEntity(
				commentsUrl(ticketId), new CreateCommentRequest("John", "Investigating the issue"), CommentResponse.class);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertNotNull(response.getBody());
		assertNotNull(response.getBody().id());
		assertNotEquals(CLIENT_COMMENT_ID, response.getBody().id());
		assertEquals(ticketId, response.getBody().ticketId());
		assertEquals("John", response.getBody().author());
		assertEquals("Investigating the issue", response.getBody().content());
		assertNotNull(response.getBody().createdAt());
		assertNotEquals(CLIENT_TIMESTAMP, response.getBody().createdAt());
	}

	@Test
	void rejectsInvalidCommentFieldsWithFieldErrors() {
		UUID ticketId = createTicket();
		CreateCommentRequest invalidRequest = new CreateCommentRequest("", "x".repeat(1001));
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
				commentsUrl(ticketId), HttpMethod.POST, new HttpEntity<>(invalidRequest, headers),
				ApiErrorResponse.class);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("VALIDATION_ERROR", response.getBody().code());
		assertEquals("/api/tickets/" + ticketId + "/comments", response.getBody().path());
		assertNotNull(response.getBody().errors());
		assertEquals(true, response.getBody().errors().containsKey("author"));
		assertEquals(true, response.getBody().errors().containsKey("content"));
	}

	@Test
	void returnsNotFoundWhenAddingCommentToMissingTicket() {
		UUID missingTicketId = UUID.randomUUID();

		ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
				commentsUrl(missingTicketId), new CreateCommentRequest("John", "Investigating the issue"),
				ApiErrorResponse.class);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("NOT_FOUND", response.getBody().code());
		assertEquals("/api/tickets/" + missingTicketId + "/comments", response.getBody().path());
	}

	@Test
	void includesCreatedCommentInTicketDetails() {
		UUID ticketId = createTicket();
		ResponseEntity<CommentResponse> created = restTemplate.postForEntity(
				commentsUrl(ticketId), new CreateCommentRequest("John", "Investigating the issue"), CommentResponse.class);

		ResponseEntity<TicketResponse> details = restTemplate.getForEntity(ticketUrl(ticketId), TicketResponse.class);

		assertEquals(HttpStatus.OK, details.getStatusCode());
		assertNotNull(details.getBody());
		assertNotNull(details.getBody().comments());
		assertEquals(1, details.getBody().comments().size());
		assertEquals(created.getBody().id(), details.getBody().comments().getFirst().id());
		assertEquals(ticketId, details.getBody().comments().getFirst().ticketId());
	}

	private UUID createTicket() {
		return ticketService.createTicket(new CreateTicketRequest("Comment test ticket", "Ticket description",
				Priority.MEDIUM, null)).id();
	}

	private String commentsUrl(UUID ticketId) {
		return ticketUrl(ticketId) + "/comments";
	}

	private String ticketUrl(UUID ticketId) {
		return "http://localhost:" + port + "/api/tickets/" + ticketId;
	}
}