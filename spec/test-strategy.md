# Support Ticket Management System Test Strategy

This strategy maps tests to the finalized requirements and acceptance criteria. It is intentionally small for a one-hour assignment: use the repository's existing test tools, deterministic fixtures, an isolated test database, and focused integration tests. Do not add performance or load tests, Testcontainers, external services, contract-testing frameworks, authentication/security suites, or other infrastructure not required by the specifications.

## Testing Levels

- **Unit tests:** Test small pure behavior in isolation, such as status-transition decisions and any focused search normalization logic. Do not duplicate HTTP, persistence, or validation-framework behavior here.
- **Service/business-rule tests:** Test ticket creation defaults, allowed and rejected state changes, update semantics, and coordination of business operations. Isolate collaborators only where useful; the state machine must also be tested through the REST API.
- **Repository/persistence tests:** Where useful, verify ticket and comment save/load behavior and query behavior against the configured H2 database. Verify persistence across restart with file-based H2 where practical.
- **REST/controller/integration tests:** Exercise the documented routes, JSON request/response DTOs, backend validation, HTTP status codes, error structure, not-found behavior, search/filter combinations, and state transitions. These tests must use the real application boundary and test database rather than mocking the controller or service behavior under test.
- **Frontend tests:** Keep tests focused on practical required interactions: list loading and errors, create/edit validation errors, displaying ticket details/comments, and handling a rejected status transition. Do not introduce a frontend test framework solely for exhaustive component coverage if one is not already available.

## State-Machine Coverage

Use REST integration tests against `PATCH /api/tickets/{id}/status` for every valid and invalid transition. For a valid transition, assert the successful response and persisted resulting status. For an invalid transition, assert HTTP `409 CONFLICT`, code `INVALID_STATUS_TRANSITION`, and that a subsequent read still reports the original status.

| Starting status | Requested status | Expected result | Test |
| --- | --- | --- | --- |
| `OPEN` | `IN_PROGRESS` | Allowed | `SM-01` |
| `IN_PROGRESS` | `RESOLVED` | Allowed | `SM-02` |
| `RESOLVED` | `CLOSED` | Allowed | `SM-03` |
| `OPEN` | `CANCELLED` | Allowed | `SM-04` |
| `IN_PROGRESS` | `CANCELLED` | Allowed | `SM-05` |
| `CLOSED` | `OPEN` | Reject with 409; state unchanged | `SM-06` |
| `CLOSED` | `IN_PROGRESS` | Reject with 409; state unchanged | `SM-07` |
| `RESOLVED` | `OPEN` | Reject with 409; state unchanged | `SM-08` |
| `RESOLVED` | `CANCELLED` | Reject with 409; state unchanged | `SM-09` |
| `CANCELLED` | `OPEN` | Reject with 409; state unchanged | `SM-10` |
| `CANCELLED` | `IN_PROGRESS` | Reject with 409; state unchanged | `SM-11` |
| Each defined state | Same state | Reject with 409; state unchanged | `SM-12` |

The backend is authoritative. UI transition controls can be checked for matching available actions, but do not substitute for these REST integration tests.

## Ticket API Tests

Use REST integration tests for the route and observable HTTP contract; use unit or service tests for isolated business decisions.

| Test | Coverage |
| --- | --- |
| `TK-01` | Create a valid ticket; assert `201 CREATED`, generated UUID, initial `OPEN` status, and server-generated timestamps. |
| `TK-02` | Create with missing/invalid required title, description, or priority; assert `400 BAD_REQUEST`, code `VALIDATION_ERROR`, and field-level errors. Include approved minimum/maximum length boundaries and out-of-range lengths. |
| `TK-03` | Get an existing ticket; assert `200 OK` and ticket details. |
| `TK-04` | Get a nonexistent ticket; assert `404 NOT_FOUND` and the common error structure. |
| `TK-05` | Update title, description, priority, and assignee using `PUT`; assert updated resource and persisted fields. |
| `TK-06` | Attempt to include or change `status` in `PUT`; verify status is not changed through this endpoint. |
| `TK-07` | Search by a substring appearing in title and by one appearing in description; assert matching results. |
| `TK-08` | Search with different letter casing; assert case-insensitive matching. |
| `TK-09` | Supply a blank or whitespace-only keyword; assert it applies no keyword filter. |
| `TK-10` | Filter by each relevant status value; assert only matching tickets are returned. |
| `TK-11` | Supply keyword and status together; assert both filters apply to results. |
| `TK-12` | Create and update without authentication with assignee omitted and with a String value; assert omission is accepted and supplied values are stored. For `PUT`, also verify omitted/null assignee clears it as specified. |

## Comment Tests

| Test | Coverage |
| --- | --- |
| `CM-01` | Add a valid comment; assert `201 CREATED`, supplied author/content, UUID comment ID, server-generated UTC timestamp, and relationship to the ticket. |
| `CM-02` | Submit missing author/content or content outside 1-1000 characters; assert `400 BAD_REQUEST`, `VALIDATION_ERROR`, and field-level errors. |
| `CM-03` | Add a comment for a nonexistent ticket; assert `404 NOT_FOUND` and the common error structure. |
| `CM-04` | Verify the comment ID and timestamp are generated by the server rather than taken from client input. |

## Persistence Tests

- Verify created and updated ticket fields can be read back from the repository/database.
- Verify comments can be read back with their owning ticket relationship.
- Where practical, use a temporary file-based H2 database, close/restart the application context against the same database file, and verify ticket and comment records remain available. Do not use in-memory-only H2 for this persistence-across-restart check.
- Keep test database files isolated from local development data and clean them up after the test.

## Error-Handling Tests

Across the REST integration tests, assert:

- `400 BAD_REQUEST` responses use code `VALIDATION_ERROR` and include the common fields `timestamp`, `status`, `code`, `message`, and `path`, plus field-level `errors`.
- `404 NOT_FOUND` responses use the common error structure and code `NOT_FOUND`.
- `409 CONFLICT` for invalid transitions uses the common structure and code `INVALID_STATUS_TRANSITION`.
- Unexpected failures, where a controlled test is practical, do not expose stack traces, SQL, internal class names, or exception messages to the client. Avoid contrived tests requiring new fault-injection infrastructure.

## Frontend Checks

When a frontend test setup already exists, cover the required UI behavior with a small set of focused tests:

- `FE-01`: Submit ticket creation and display the returned ticket.
- `FE-02`: Display the ticket list, apply keyword and status filters together, and treat a blank keyword as no keyword filter.
- `FE-03`: Display ticket details and comments.
- `FE-04`: Edit ticket fields without exposing status as editable; show successful updates.
- `FE-05`: Submit a comment and display the returned comment.
- `FE-06`: Offer only allowed status actions, handle `409 CONFLICT` meaningfully, and refresh/display status after success or conflict.
- `FE-07`: Display backend validation errors, not-found errors, loading states, and unexpected errors without exposing internal details.

If frontend automation is not already set up, prioritize backend REST integration coverage and perform a concise manual UI check for the required flows rather than introducing substantial test infrastructure.

## Deterministic Test Data

- Use fixed, descriptive ticket and comment fixtures with known UUIDs where IDs are controllable, or assert only the UUID format for server-generated IDs.
- Use a controlled clock or assert timestamp format and presence without relying on exact wall-clock timing.
- Keep data isolated per test; do not rely on execution order, external services, real network calls, random data, arbitrary sleeps, or shared mutable fixtures.
- Assert observable responses and persisted outcomes rather than private implementation details.

## Acceptance-Criteria Traceability

| Assignment acceptance criterion | Test coverage |
| --- | --- |
| A ticket can be created from the UI. | `FE-01`; `TK-01` API creation. |
| Tickets can be listed. | `FE-02`; `TK-10` list/filter route coverage. |
| Ticket details can be viewed. | `FE-03`; `TK-03`. |
| Ticket fields can be updated. | `FE-04`; `TK-05`. |
| Assignee can be changed. | `FE-04`; `TK-05`, `TK-12`; verify omitted/null clearing according to the API contract. |
| Comments can be added. | `FE-05`; `CM-01`. |
| Search works. | `TK-07`, `TK-08`, `TK-09`; `FE-02`. |
| Status filter works. | `TK-10`; `FE-02`. |
| Valid status transitions work. | `SM-01` through `SM-05`; `FE-06`. |
| Invalid status transitions are rejected by backend. | `SM-06` through `SM-12`, including HTTP 409 and unchanged status. |
| Data survives application restart. | Persistence restart check using file-based H2. |
| Backend validation works. | `TK-02`, `CM-02`, and invalid status input in REST tests. |
| Validation failures return HTTP 400 with field-level errors. | `TK-02`, `CM-02`; common error-structure assertions. |
| Requests for missing tickets return HTTP 404. | `TK-04`, `CM-03`; not-found error assertions. |
| Invalid status transitions return HTTP 409. | `SM-06` through `SM-12`; `FE-06` for user-facing handling. |
| API errors use the consistent JSON structure. | Shared assertions in `TK-02`, `TK-04`, `SM-06` through `SM-12`, and `CM-02`/`CM-03`. |
| Local file-based H2 data survives application restart. | File-based H2 restart persistence check for ticket and comment records. |
| UI shows meaningful errors. | `FE-07`; concise manual check if frontend tests are unavailable. |
| State-machine integration tests pass. | REST integration tests `SM-01` through `SM-09`. |
| No secrets are committed. | Repository diff/secret scan during review; this is a repository hygiene check, not an application behavior test. |

## Open Questions

None. The finalized specifications define the behaviors covered by this strategy.