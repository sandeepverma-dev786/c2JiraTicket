# Support Ticket Management System Architecture

## High-Level Architecture

The application is a single frontend backed by a single Spring Boot REST service and a relational database:

```text
React/Vite frontend  <---- JSON over HTTP ---->  Spring Boot REST backend  <---->  File-based H2
```

- **Frontend:** React with Vite. Provides ticket creation, listing, details, updates, comments, keyword search, status filtering, status changes, and user-visible error messages.
- **Backend:** Java 21 and Spring Boot. Exposes the REST API, validates incoming data, applies ticket business rules, enforces the state machine, and translates expected failures to the standard JSON error format.
- **Persistence:** File-based H2 for local development. Ticket and comment data must remain available after an application restart.

Keep this as a simple layered application. The frontend communicates with the backend through HTTP and does not access the database directly.

## Backend Package Structure

Use packages within the application's existing root package. A suitable minimal structure is:

```text
<application-root>/
  controller/   HTTP endpoints and request/response binding
  service/      use cases, business rules, and state-transition validation
  repository/   persistence access
  entity/       persisted ticket and comment models
  dto/          API request and response models
  exception/    application errors and centralized HTTP error translation
  config/       only required application configuration
  mapper/       only if explicit mapping logic warrants a separate package
```

Controllers should handle HTTP concerns and delegate use cases to services. Services own business decisions and coordinate repositories. Repositories perform persistence operations. DTOs define API boundaries; persistence entities are not exposed directly as API responses. Use constructor injection between layers.

Keep `config` and `mapper` only when there is actual configuration or mapping to maintain. Do not create additional layers or abstractions speculatively.

## Frontend Structure

Organize the React/Vite frontend around a small set of responsibilities:

```text
src/
  pages/ or views/   ticket list, ticket details, and ticket creation/editing views
  components/        reusable ticket, comment, filter, and error UI
  api/               HTTP calls and API response types
  types/             shared frontend data shapes, if not kept with API calls
```

The frontend should call the REST API for reads and writes, display field-level validation errors and other meaningful API errors, and may provide convenient client-side validation. Client-side validation is supplementary; the backend remains authoritative. Use the project's chosen structure consistently and avoid introducing a state-management framework unless the implementation demonstrates a need.

## Request Flow

```text
UI -> REST Controller -> Service -> Repository -> File-based H2
```

The controller binds and validates the request DTO, delegates the operation to a service, and returns an API response DTO. The service applies the relevant business rules and calls the repository. The repository loads or stores ticket and comment entities in H2. Responses travel back through the service and controller to the UI.

## State Transition Flow

```text
UI -> PATCH /api/tickets/{id}/status -> Controller -> Service state-machine validation -> Repository -> File-based H2
```

The request body supplies the target status. The service checks the current state against the transitions in [state-machine.md](state-machine.md) before persisting a change. The backend is authoritative; frontend controls must not be relied on to enforce valid transitions. Invalid transitions leave the ticket unchanged and return HTTP `409 CONFLICT` in the standard API error format. Valid transitions have no additional preconditions or required side effects.

## Validation and Error Flow

The backend validates request data at the API boundary and enforces business invariants in the service layer. Expected failures are translated centrally into the consistent JSON error response defined in [requirements.md](requirements.md): `timestamp`, numeric `status`, `code`, `message`, and `path`, with field-level `errors` for validation failures.

- **Validation failure:** Return HTTP `400 BAD_REQUEST` with code `VALIDATION_ERROR` and field-level errors. The UI presents errors meaningfully to the user.
- **Ticket not found:** Return HTTP `404 NOT_FOUND` using the same general JSON error structure.
- **Invalid status transition:** Return HTTP `409 CONFLICT` with code `INVALID_STATUS_TRANSITION` using the same general JSON error structure. Do not change the ticket.
- **Unexpected failure:** Do not expose internal exception details to the client; log safely for diagnosis.

## Persistence

Use file-based H2 for local development, not in-memory H2. Persist ticket and comment data so that it survives application restarts. Keep database access behind the repository layer. No separate cache or messaging system is required.

## API Design

Use normal REST semantics and JSON request and response bodies for these routes:

| Method | Route | Purpose |
| --- | --- | --- |
| `POST` | `/api/tickets` | Create a ticket |
| `GET` | `/api/tickets` | List tickets; optional search and status filters may be supplied |
| `GET` | `/api/tickets/{id}` | Get ticket details |
| `PUT` | `/api/tickets/{id}` | Update a ticket |
| `PATCH` | `/api/tickets/{id}/status` | Request a ticket status transition |
| `POST` | `/api/tickets/{id}/comments` | Add a comment to a ticket |

The list route accepts optional `keyword` and `status` query parameters:

```text
GET /api/tickets?keyword={keyword}&status={status}
```

`keyword` uses case-insensitive substring matching against title and description. A blank or whitespace-only keyword applies no keyword filter. `status` uses the defined ticket status values. Either parameter may be supplied independently, or both may be supplied together. Do not add a ticket deletion route, authentication endpoints, or other endpoints not required by the assignment.