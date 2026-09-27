# Support Ticket Management System UI Flow

This document describes a simple user flow based on the finalized [requirements](requirements.md), [state machine](state-machine.md), [architecture](architecture.md), [data model](data-model.md), and [API contract](api-contract.md). It defines behavior, not visual styling or implementation components.

## Ticket List

On entry, load tickets using `GET /api/tickets` and display the returned tickets, including their current status. Show a loading state while the request is in progress, an empty state when there are no matching tickets, and an error state if loading fails.

The list provides:

- A keyword field and a status filter, plus a create-ticket action.
- Search through `GET /api/tickets?keyword={keyword}&status={status}`. Send either optional query parameter independently or both together. Omit a blank or whitespace-only keyword so it applies no keyword filter. Search is case-insensitive substring matching on title and description.
- A way to open a ticket's details. Selecting a ticket loads `GET /api/tickets/{id}`.

While a search or filter request is loading, indicate that the results are updating. If it fails, show a meaningful error and retain the available controls so the user can correct the query or retry. Do not add pagination.

## Create Ticket

From the list, the user opens the create-ticket form and enters:

- Title (required, 1-200 characters)
- Description (required, 1-2000 characters)
- Priority (required: `LOW`, `MEDIUM`, `HIGH`, or `CRITICAL`)
- Assignee (optional String)

On submit, send `POST /api/tickets` with those editable fields. Do not ask for an ID, status, or timestamps; the server generates these and starts the ticket in `OPEN`.

Show a submitting state while waiting. On success, show the created ticket, including its server-assigned ID and `OPEN` status, by opening its details. On HTTP `400 BAD_REQUEST`, display backend field-level validation messages next to the relevant fields and preserve the entered values.

## Ticket Details

Load and display the ticket's fields, current status, server-generated timestamps, and comments using `GET /api/tickets/{id}`. Provide actions to edit the ticket and add a comment. Show only the status-transition actions allowed from its current status.

If the ticket cannot be loaded because it does not exist, show the not-found state. For other failures, show a meaningful non-technical error and allow retrying the load.

## Edit Ticket

The edit flow allows changes to title, description, priority, and assignee only. Status is displayed separately and is not editable in this form.

On submit, send `PUT /api/tickets/{id}` with the complete editable ticket fields. Title, description, and priority are required. Assignee is optional and nullable; if omitted or `null`, it is cleared. Do not send or change status, ID, `createdAt`, or `updatedAt`.

Show a submitting state. On success, display the returned updated ticket details. On HTTP `400 BAD_REQUEST`, display field-level errors and retain the user's input. On HTTP `404 NOT_FOUND`, show that the ticket could not be found. Display a safe, meaningful message for unexpected errors.

## Status Transition

Offer only actions valid for the currently displayed status:

| Current status | Available actions |
| --- | --- |
| `OPEN` | `IN_PROGRESS`, `CANCELLED` |
| `IN_PROGRESS` | `RESOLVED`, `CANCELLED` |
| `RESOLVED` | `CLOSED` |
| `CLOSED` | None |
| `CANCELLED` | None |

When an action is selected, send `PATCH /api/tickets/{id}/status` with `{ "status": "<target status>" }`. The UI's available actions are guidance only; the backend is authoritative and validates every transition.

On success, display the returned ticket with its new status. If the backend returns HTTP `409 CONFLICT` with code `INVALID_STATUS_TRANSITION`, explain that the status change is no longer allowed and reload the ticket to show its current status. The server leaves the ticket unchanged for an invalid transition. Also handle HTTP `404 NOT_FOUND` if the ticket no longer exists.

## Add Comment

On ticket details, provide required `author` and `content` fields. Comment content must contain 1-1000 characters. Submit using `POST /api/tickets/{id}/comments` with only `author` and `content`; the server generates the comment ID, associates it with the ticket, and generates `createdAt`.

Show a submitting state. On success, display the returned comment in the ticket's comments. On HTTP `400 BAD_REQUEST`, show field-level validation errors and preserve input. On HTTP `404 NOT_FOUND`, explain that the ticket could not be found. Display a safe, meaningful message for unexpected errors.

## Shared Error Behavior

- **HTTP 400 validation error:** Use the API error's field-level `errors` to identify and explain invalid fields. Keep the form open and preserve entered values.
- **HTTP 404 not found:** Clearly state that the requested ticket is unavailable; do not display internal details.
- **HTTP 409 invalid transition:** Explain that the requested status change is not allowed, then refresh the ticket status.
- **Unexpected server error:** Show a concise, non-technical message without stack traces or internal exception details. Keep the user on the current flow when possible.
- **Loading and submission:** Indicate ongoing requests and prevent ambiguous duplicate submissions while a form request is in progress.

## Open Questions

None. The required UI actions and their API behaviors are defined by the finalized specifications.