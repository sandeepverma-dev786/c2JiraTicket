# Support Ticket Management System Implementation Plan

This plan follows the finalized specifications and repository rules. Implement only the required behavior; do not add authentication, Kafka, Redis, notifications, dashboards, pagination, Docker, microservices, cloud infrastructure, or other out-of-scope features. In particular, the backend state machine is authoritative: frontend transition controls are guidance and must not replace backend enforcement.

Paths such as `<backend-root>` and `<frontend-root>` are placeholders for the chosen project layout. Create only files needed for the listed responsibilities.

## Sequential Tasks

### Task 01: Project and Backend Foundation

- **Purpose:** Confirm the approved specifications as the implementation baseline; establish the minimal Java 21/Spring Boot backend application and build configuration.
- **Files/packages likely to be created or changed:** `<backend-root>/` build and application configuration files; application bootstrap under the chosen root package.
- **Dependencies/prerequisites:** All specification and guidance documents approved; none of the application layers exists yet.
- **Validation/test expected:** Backend builds and starts with its test profile; verify Java 21 and Spring Boot configuration. No business behavior is added here.

### Task 02: Domain Enums and Entities

- **Purpose:** Define the ticket and comment domain/persistence shapes and approved value sets.
- **Files/packages likely to be created or changed:** `<application-root>/entity/Ticket`, `Comment`; `<application-root>/entity/TicketStatus`, `Priority` (or equivalent domain package).
- **Dependencies/prerequisites:** Task 01.
- **Validation/test expected:** Compile and add focused model checks if the chosen persistence mapping requires them; confirm all enum values and fields match `spec/data-model.md`.

### Task 03: Repository and Persistence

- **Purpose:** Persist and retrieve tickets and comments using file-based H2 for local development.
- **Files/packages likely to be created or changed:** `<application-root>/repository/`; minimal H2 configuration in application properties; repository-focused tests.
- **Dependencies/prerequisites:** Task 02 entities; Task 01 build setup.
- **Validation/test expected:** Repository tests save and reload tickets/comments and their relationship. Explicitly verify persistence using the same file-based H2 database across an application restart; in-memory H2 is not sufficient.

### Task 04: DTOs and API Models

- **Purpose:** Define request and response models separate from persistence entities.
- **Files/packages likely to be created or changed:** `<application-root>/dto/` request/response DTOs for ticket create/update, status change, comment creation, ticket/list/detail responses, and common API errors.
- **Dependencies/prerequisites:** Tasks 02-03 and the finalized API/data-model specifications.
- **Validation/test expected:** Compile and verify DTO fields exclude client control of generated IDs/timestamps and prevent status changes through the ticket update request.

### Task 05: Exception and Error Handling

- **Purpose:** Translate validation, not-found, invalid-transition, and unexpected failures into the specified consistent JSON error response.
- **Files/packages likely to be created or changed:** `<application-root>/exception/` application exceptions and centralized REST exception handler; common error DTO as needed.
- **Dependencies/prerequisites:** Task 04 error/request DTOs.
- **Validation/test expected:** Focused tests assert `400 VALIDATION_ERROR` with field errors, `404 NOT_FOUND`, `409 INVALID_STATUS_TRANSITION`, common error fields, and no internal exception details.

### Task 06: Ticket Service and State-Machine Enforcement

- **Purpose:** Implement ticket use cases and the sole authority for valid state transitions. New tickets start in `OPEN`; valid transitions follow `spec/state-machine.md` and have no extra preconditions or required side effects.
- **Files/packages likely to be created or changed:** `<application-root>/service/` ticket service and a small transition decision component only if it clarifies the implementation.
- **Dependencies/prerequisites:** Tasks 02-05.
- **Validation/test expected:** Unit/service tests cover creation defaults, edit semantics, every valid transition, rejected transitions, and unchanged state after rejection. The backend, not the UI, enforces transitions.

### Task 07: Ticket REST APIs

- **Purpose:** Expose create, list/search/filter, details, update, and status-transition operations using the finalized routes and JSON contract.
- **Files/packages likely to be created or changed:** `<application-root>/controller/TicketController`; ticket request/response DTOs and direct mapping where needed.
- **Dependencies/prerequisites:** Tasks 04-06.
- **Validation/test expected:** REST/controller integration tests check routes, response shapes and statuses, title/description/priority constraints, case-insensitive substring search, blank keyword behavior, combined status/keyword filtering, not-found behavior, and that `PUT` cannot change status.

### Task 08: Comment Functionality

- **Purpose:** Add comments to an existing ticket, with server-generated UUID and timestamp and client-supplied author/content.
- **Files/packages likely to be created or changed:** Comment operations in `<application-root>/service/` and `<application-root>/controller/` (or the existing ticket controller if simpler); comment DTOs and repository methods as needed.
- **Dependencies/prerequisites:** Tasks 02-07.
- **Validation/test expected:** Tests cover valid comment creation, required author/content and content length, `404` for a missing ticket, server-generated ID/timestamp, and association with the owning ticket.

### Task 09: Backend Unit and Integration Tests

- **Purpose:** Complete focused backend verification at the unit, service, repository, and REST boundaries.
- **Files/packages likely to be created or changed:** Existing test source tree, using the repository's current test framework; no new testing infrastructure unless already present.
- **Dependencies/prerequisites:** Tasks 01-08.
- **Validation/test expected:** Run the backend test suite. REST integration coverage must exercise all five valid transitions, every invalid transition listed in `spec/state-machine.md`, same-state rejection for all statuses, HTTP `409`, and unchanged persisted status after rejection. Verify consistent `400`, `404`, and `409` responses.

### Task 10: Frontend Foundation

- **Purpose:** Establish the minimal React/Vite frontend and API access structure.
- **Files/packages likely to be created or changed:** `<frontend-root>/` package/build configuration and app entry; `src/api/`, `src/types/`, and a minimal view structure.
- **Dependencies/prerequisites:** Task 01 project layout; finalized API contract.
- **Validation/test expected:** Frontend builds and starts; API base configuration is usable in the local environment. Avoid adding global state libraries without a demonstrated need.

### Task 11: Ticket List, Search, and Filter UI

- **Purpose:** Display tickets, support keyword and status filters together, provide the create action, and open ticket details.
- **Files/packages likely to be created or changed:** `<frontend-root>/src/pages/` or `views/` list screen; reusable list/filter components; API client functions.
- **Dependencies/prerequisites:** Task 10 and Task 07 ticket list API.
- **Validation/test expected:** Check list loading, empty, and error states; keyword matching and blank-keyword behavior; status-only and combined filters; opening ticket details. No pagination.

### Task 12: Create and Edit Ticket UI

- **Purpose:** Provide required ticket creation and complete editable-field update flows.
- **Files/packages likely to be created or changed:** Frontend create/edit views and form components; API client functions and request types.
- **Dependencies/prerequisites:** Tasks 10-11 and Tasks 07-09 API behavior.
- **Validation/test expected:** Check required title (1-200), description (1-2000), priority enum, and optional assignee. Verify create omits server-generated fields and success displays the new `OPEN` ticket. Verify `PUT` submits title/description/priority and nullable assignee semantics, never status; display backend field errors and `404` meaningfully.

### Task 13: Ticket Details and Status-Transition UI

- **Purpose:** Display ticket fields, timestamps, status, and comments; expose only currently valid transition actions.
- **Files/packages likely to be created or changed:** Ticket detail view and status action UI; API client functions/types.
- **Dependencies/prerequisites:** Tasks 10-12; Task 07 detail/status APIs.
- **Validation/test expected:** Check detail loading and not-found state; verify displayed actions match the state machine. Send `PATCH /api/tickets/{id}/status`; refresh/display the returned status on success and handle `409` by explaining the conflict and reloading. Backend remains authoritative.

### Task 14: Comment UI

- **Purpose:** Let a user submit an author and comment content from ticket details and display the returned comment.
- **Files/packages likely to be created or changed:** Comment form and comment display components; comment API client function/types.
- **Dependencies/prerequisites:** Tasks 10, 13, and 08.
- **Validation/test expected:** Check required author/content, 1-1000 content length, `400` field errors, `404` for missing ticket, and successful display of server-generated comment ID/timestamp.

### Task 15: Frontend/Backend Integration

- **Purpose:** Connect all UI flows to the documented REST API and align request/response handling.
- **Files/packages likely to be created or changed:** Frontend API client, minimal local API-origin configuration, and only necessary backend development configuration.
- **Dependencies/prerequisites:** Tasks 07-08 and 10-14.
- **Validation/test expected:** Run frontend and backend together; exercise create, list, search/filter, detail, update, status change, and comment paths; verify all error states use safe user-facing messages.

### Task 16: End-to-End Acceptance Validation

- **Purpose:** Verify each approved assignment acceptance criterion with automated tests where practical and concise manual UI checks for user-visible flows.
- **Files/packages likely to be created or changed:** Existing integration/UI test sources; test fixtures/config only as needed. Record results in the delivery/review notes, not as invented requirements.
- **Dependencies/prerequisites:** Tasks 01-15.
- **Validation/test expected:** Check every acceptance criterion:
  - [ ] AC-01: Create a ticket from the UI.
  - [ ] AC-02: List tickets.
  - [ ] AC-03: View ticket details.
  - [ ] AC-04: Update ticket fields, including assignee.
  - [ ] AC-05: New tickets start in `OPEN`.
  - [ ] AC-06: Assignee can be omitted or supplied as a String without authentication or a User entity.
  - [ ] AC-07: Add comments with required fields; author is client-supplied and ID/timestamp are server-generated; comment content limits work.
  - [ ] AC-08: Title, description, and priority validation/value constraints work.
  - [ ] AC-09: Search is case-insensitive substring matching across title/description; blank keyword is no filter; combined filtering works.
  - [ ] AC-10: Status changes use `PATCH /api/tickets/{id}/status` with the required JSON status.
  - [ ] AC-11: Status filtering works.
  - [ ] AC-12: Every valid status transition succeeds.
  - [ ] AC-13: Invalid transitions are rejected by the backend and do not change status.
  - [ ] AC-14: Ticket and comment data survive application restart.
  - [ ] AC-15: Backend validation rejects invalid input.
  - [ ] AC-16: Validation failures return HTTP `400` with field-level errors.
  - [ ] AC-17: Missing tickets return HTTP `404`.
  - [ ] AC-18: Invalid transitions return HTTP `409` with the specified code.
  - [ ] AC-19: API errors use the consistent JSON structure.
  - [ ] AC-20: File-based H2 data survives application restart (also supplies evidence for AC-14).
  - [ ] AC-21: UI shows meaningful loading and error states without internal exception details.
  - [ ] AC-22: State-machine integration tests pass through the REST API.
  - [ ] AC-23: Repository changes contain no committed secrets; perform an appropriate repository diff/secret hygiene review.

### Task 17: Final Code/Specification Review and Issue Notes

- **Purpose:** Review the completed implementation against all approved specifications and rules; identify bugs, requirement gaps, incorrect assumptions, security/privacy issues, unnecessary complexity, and test gaps. Record actual AI-generated mistakes or issues discovered during implementation and their disposition; do not invent or pre-fill issues.
- **Files/packages likely to be created or changed:** Any implementation files needed to fix confirmed findings; `docs/implementation-notes.md` only if actual AI-generated mistakes/issues were discovered. Keep notes factual (observed issue, impact, correction); do not create fictional entries.
- **Dependencies/prerequisites:** Tasks 01-16 complete.
- **Validation/test expected:** Re-run focused tests after any fixes; confirm the acceptance checklist is complete and compare routes, fields, constraints, state transitions, errors, persistence, and UI flows against `spec/` and `rules/`. Ensure no out-of-scope features or secrets were introduced.

## Open Questions / Dependencies

No product requirements remain open. Select build tooling and the concrete root package/layout consistently with the project during Task 01; these are implementation setup choices, not additional product requirements.