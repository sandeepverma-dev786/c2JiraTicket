# Support Ticket Management System API Contract

This document defines the JSON REST contract for the required ticket and comment operations. Ticket and comment fields follow [data-model.md](data-model.md); ticket statuses and allowed transitions follow [state-machine.md](state-machine.md). Persistence entities are not exposed directly; the JSON resources below are API representations.

Ticket IDs and comment IDs are server-generated UUIDs. The server generates `createdAt` and `updatedAt`; clients must not provide or modify IDs or timestamps. Timestamp values are read-only ISO-8601 strings in UTC, for example `2026-09-27T10:30:00Z`. In the examples below, `<server-generated timestamp>` stands in for a value in this format. New tickets start with status `OPEN`. No authentication, deletion, or pagination endpoints are included.

## Common Error Response

Errors use the consistent JSON structure defined in [requirements.md](requirements.md). Validation errors also contain field-level `errors`.

Validation error example:

```json
{
  "timestamp": "<server-generated timestamp>",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "errors": {
    "title": "Title is required"
  },
  "path": "/api/tickets"
}
```

Not-found error example:

```json
{
  "timestamp": "<server-generated timestamp>",
  "status": 404,
  "code": "NOT_FOUND",
  "message": "Ticket not found",
  "path": "/api/tickets/550e8400-e29b-41d4-a716-446655440000"
}
```

## Create Ticket

- **Method:** `POST`
- **Path:** `/api/tickets`
- **Query parameters:** None
- **Request JSON:** `title`, `description`, and `priority` are required. `assignee` is optional. Do not send `id`, `status`, `createdAt`, or `updatedAt`; the server generates the ID and timestamps and assigns `OPEN` status.

```json
{
  "title": "Cannot access account",
  "description": "The sign-in page returns an error.",
  "priority": "HIGH",
  "assignee": "Support Team"
}
```

- **Success:** `201 CREATED`
- **Response JSON:** Created ticket resource.

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Cannot access account",
  "description": "The sign-in page returns an error.",
  "priority": "HIGH",
  "status": "OPEN",
  "assignee": "Support Team",
  "createdAt": "<server-generated timestamp>",
  "updatedAt": "<server-generated timestamp>"
}
```

- **Possible errors:** `400 BAD_REQUEST` for validation failures, using code `VALIDATION_ERROR` and field-level errors.

## List Tickets, Search, and Filter

- **Method:** `GET`
- **Path:** `/api/tickets`
- **Query parameters:** Optional `keyword` and `status`.
  - `keyword` uses case-insensitive substring matching against ticket title and description. A blank or whitespace-only value applies no keyword filter.
  - `status` must be one of `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, or `CANCELLED`.
  - Either parameter can be used alone; when both are provided, both filters apply.
- **Request JSON:** None.
- **Success:** `200 OK`
- **Response JSON:** JSON array of ticket resources; an empty result is `[]`.

Example request:

```text
GET /api/tickets?keyword=sign-in&status=OPEN
```

Example response:

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "title": "Cannot access account",
    "description": "The sign-in page returns an error.",
    "priority": "HIGH",
    "status": "OPEN",
    "assignee": "Support Team",
    "createdAt": "<server-generated timestamp>",
    "updatedAt": "<server-generated timestamp>"
  }
]
```

- **Possible errors:** `400 BAD_REQUEST` with code `VALIDATION_ERROR` if `status` is not a defined status value.

## Get Ticket Details

- **Method:** `GET`
- **Path:** `/api/tickets/{id}` where `id` is the ticket UUID.
- **Query parameters:** None.
- **Request JSON:** None.
- **Success:** `200 OK`
- **Response JSON:** Ticket resource with its comments in a `comments` array.

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Cannot access account",
  "description": "The sign-in page returns an error.",
  "priority": "HIGH",
  "status": "OPEN",
  "assignee": "Support Team",
  "createdAt": "<server-generated timestamp>",
  "updatedAt": "<server-generated timestamp>",
  "comments": [
    {
      "id": "7b8e6f40-2e9a-4d1f-bb94-4a1e9a8bb4f2",
      "ticketId": "550e8400-e29b-41d4-a716-446655440000",
      "author": "Customer",
      "content": "I started seeing this today.",
      "createdAt": "<server-generated timestamp>"
    }
  ]
}
```

- **Possible errors:** `404 NOT_FOUND` if the ticket does not exist, using the common error response.

## Update Ticket

- **Method:** `PUT`
- **Path:** `/api/tickets/{id}` where `id` is the ticket UUID.
- **Query parameters:** None.
- **Request JSON:** This is a complete update of the editable ticket fields. `title`, `description`, and `priority` are required; `assignee` is optional and nullable. If `assignee` is omitted or `null`, the assignee is cleared. `status` must not be changed through this endpoint. Do not send `id`, `createdAt`, or `updatedAt`.

```json
{
  "title": "Sign-in error",
  "description": "The sign-in page returns an error after submitting credentials.",
  "priority": "CRITICAL",
  "assignee": "Account Support"
}
```

- **Success:** `200 OK`
- **Response JSON:** Updated ticket resource. The server maintains `id`, `status`, `createdAt`, and `updatedAt`; status is not changed by this operation.

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Sign-in error",
  "description": "The sign-in page returns an error after submitting credentials.",
  "priority": "CRITICAL",
  "status": "OPEN",
  "assignee": "Account Support",
  "createdAt": "<server-generated timestamp>",
  "updatedAt": "<server-generated timestamp>"
}
```

- **Possible errors:** `400 BAD_REQUEST` with code `VALIDATION_ERROR` and field-level errors; `404 NOT_FOUND` if the ticket does not exist.

## Change Ticket Status

- **Method:** `PATCH`
- **Path:** `/api/tickets/{id}/status` where `id` is the ticket UUID.
- **Query parameters:** None.
- **Request JSON:** Required target `status`, one of the defined `TicketStatus` values.

```json
{
  "status": "IN_PROGRESS"
}
```

- **Valid transitions:** `OPEN` to `IN_PROGRESS`; `OPEN` to `CANCELLED`; `IN_PROGRESS` to `RESOLVED`; `IN_PROGRESS` to `CANCELLED`; `RESOLVED` to `CLOSED`.
- **Success:** `200 OK`
- **Response JSON:** Updated ticket resource, with its new status.

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Cannot access account",
  "description": "The sign-in page returns an error.",
  "priority": "HIGH",
  "status": "IN_PROGRESS",
  "assignee": "Support Team",
  "createdAt": "<server-generated timestamp>",
  "updatedAt": "<server-generated timestamp>"
}
```

- **Possible errors:** `400 BAD_REQUEST` with code `VALIDATION_ERROR` if the request is malformed or `status` is not a defined value; `404 NOT_FOUND` if the ticket does not exist; `409 CONFLICT` with code `INVALID_STATUS_TRANSITION` if the requested transition is not allowed.
- An invalid transition must leave the ticket unchanged. The error uses the common JSON structure.

Invalid-transition error example:

```json
{
  "timestamp": "<server-generated timestamp>",
  "status": 409,
  "code": "INVALID_STATUS_TRANSITION",
  "message": "Ticket cannot transition from CLOSED to OPEN",
  "path": "/api/tickets/550e8400-e29b-41d4-a716-446655440000/status"
}
```

## Add Comment

- **Method:** `POST`
- **Path:** `/api/tickets/{id}/comments` where `id` is the ticket UUID.
- **Query parameters:** None.
- **Request JSON:** Required `author` and `content`. `content` must contain 1-1000 characters. Do not send `id`, `ticketId`, or `createdAt`; the server generates or supplies these values.

```json
{
  "author": "Customer",
  "content": "I started seeing this today."
}
```

- **Success:** `201 CREATED`
- **Response JSON:** Created comment resource.

```json
{
  "id": "7b8e6f40-2e9a-4d1f-bb94-4a1e9a8bb4f2",
  "ticketId": "550e8400-e29b-41d4-a716-446655440000",
  "author": "Customer",
  "content": "I started seeing this today.",
  "createdAt": "<server-generated timestamp>"
}
```

- **Possible errors:** `400 BAD_REQUEST` with code `VALIDATION_ERROR` and field-level errors if `author` or `content` is missing or `content` is outside its allowed length; `404 NOT_FOUND` if the ticket does not exist.

## Open Questions

None. The previously open timestamp and `PUT` assignee decisions are resolved above.