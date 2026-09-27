# Support Ticket Management System Requirements

## Scope

The system provides a user interface and REST API for creating, finding, viewing, and maintaining support tickets. Ticket data must be stored in a database and remain available after an application restart. The system must enforce the specified ticket status workflow on the backend.

Technology constraints are Java 21, Spring Boot, PostgreSQL or H2, a REST API, and React/Next.js or an equivalent frontend. Use file-based H2 for local development so persisted data survives an application restart. Other environments may use PostgreSQL or H2. Except for the status-transition route defined below, these constraints do not prescribe endpoint shapes or screen layout.

## Functional Requirements

1. A user can create a ticket from the UI.
2. A user can list tickets.
3. A user can view the details of a ticket.
4. A user can update a ticket's title, description, priority, and assignee. Assignee is an optional String field.
5. A user can add comments to a ticket. Each comment contains `id`, `ticketId`, `author`, `content`, and `createdAt`. The server generates the comment `id` as a UUID and generates `createdAt`; the client supplies `author` as a String.
6. A user can search tickets by keyword in the title and description using case-insensitive substring matching. A blank or whitespace-only keyword applies no search filter. Search can be combined with status filtering.
7. A user can filter tickets by status.
8. The system persists ticket data in a database so that it survives application restart.
9. The backend validates input according to the validation requirements below.
10. A client requests a status transition using `PATCH /api/tickets/{id}/status` with a JSON body containing the target `status`. The backend accepts only valid transitions and rejects invalid transitions, as defined in [state-machine.md](state-machine.md).
11. The UI displays meaningful errors when an operation fails.

## Non-Functional Requirements

- The backend uses Java 21 and Spring Boot and exposes a REST API.
- The persistence technology is PostgreSQL or H2.
- The frontend uses React, Next.js, or an equivalent technology.
- Local development uses file-based H2 rather than in-memory H2 so that data persists across application restarts.
- Ticket data persists across application restarts; an in-memory-only persistence configuration does not satisfy this requirement.
- No User entity or authentication is required. The assignee is a String, not a reference to a User entity.
- No secrets are committed to the repository.
- No additional performance, availability, accessibility, or deployment targets are required beyond the original assignment.

## Validation Requirements

- Validate input on the backend; client-side validation alone is insufficient.
- Ticket title is required and must contain 1-200 characters.
- Ticket description is required and must contain 1-2000 characters.
- Ticket priority is required and must be one of `LOW`, `MEDIUM`, `HIGH`, or `CRITICAL`.
- Ticket assignee is an optional String.
- Comment content is required and must contain 1-1000 characters.
- Comment `id` is a server-generated UUID, `author` is supplied by the client as a String, and `createdAt` is server-generated.
- Reject invalid status transitions on the backend.
- No additional field constraints are specified.

## Error Handling Requirements

- When an operation fails, the UI presents an error that is meaningful to the user and does not expose internal implementation details.
- API errors use a consistent JSON structure with `timestamp`, numeric `status`, machine-readable `code`, `message`, and `path`. Validation errors also include an `errors` object mapping fields to messages.
- Validation failures return HTTP `400 BAD_REQUEST` with code `VALIDATION_ERROR` and field-level error information.
- A ticket not found returns HTTP `404 NOT_FOUND` using the same general error structure.
- An invalid status transition returns HTTP `409 CONFLICT` with code `INVALID_STATUS_TRANSITION` using the same general error structure.
- Example validation error:

	```json
	{
		"timestamp": "...",
		"status": 400,
		"code": "VALIDATION_ERROR",
		"message": "Request validation failed",
		"errors": {
			"title": "Title is required"
		},
		"path": "/api/tickets"
	}
	```

- Example invalid-transition error:

	```json
	{
		"timestamp": "...",
		"status": 409,
		"code": "INVALID_STATUS_TRANSITION",
		"message": "Ticket cannot transition from CLOSED to OPEN",
		"path": "/api/tickets/123/status"
	}
	```

- A ticket-not-found error uses the same structure with status `404` and code `NOT_FOUND`.

## Acceptance Criteria

- A ticket can be created from the UI.
- Tickets can be listed and ticket details can be viewed.
- Ticket fields can be updated, including changing the assignee.
- A ticket can be created with `OPEN` status.
- The assignee can be omitted or supplied as a String without requiring authentication or a User entity.
- Comments can be added with the specified fields; the server generates a UUID `id` and `createdAt`, and the client supplies `author` as a String. Comment content is validated to 1-1000 characters.
- Ticket title, description, and priority satisfy their specified validation rules; priority is one of `LOW`, `MEDIUM`, `HIGH`, or `CRITICAL`.
- Ticket keyword search uses case-insensitive substring matching across title and description; blank or whitespace-only keywords apply no search filter, and search can be combined with status filtering.
- Status changes use `PATCH /api/tickets/{id}/status` with a JSON `status` field.
- Filtering by status works.
- Valid status transitions work.
- Invalid status transitions are rejected by the backend.
- Data survives application restart.
- Backend validation works.
- Validation failures return HTTP `400 BAD_REQUEST` with field-level error information.
- Requests for missing tickets return HTTP `404 NOT_FOUND`.
- Invalid status transitions return HTTP `409 CONFLICT`.
- API errors use the specified consistent JSON structure, including field-level `errors` for validation failures.
- Local file-based H2 data survives application restart.
- The UI shows meaningful errors.
- State-machine integration tests pass.
- No secrets are committed.

## Out of Scope

Unless later specified, this system does not include authentication, User entities, roles or permission management, notifications, dashboards, pagination, Kafka, Redis, ticket deletion, or comment editing and deletion. These items must not be inferred as requirements.
