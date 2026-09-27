# Ticket State Machine

This document defines the allowed ticket status transitions. The backend is authoritative and must reject transitions not listed as valid.

New tickets always start in `OPEN`.

## States

- `OPEN`
- `IN_PROGRESS`
- `RESOLVED`
- `CLOSED`
- `CANCELLED`

## Valid Transitions

| Current state | Requested state | Result |
| --- | --- | --- |
| `OPEN` | `IN_PROGRESS` | Allowed |
| `OPEN` | `CANCELLED` | Allowed |
| `IN_PROGRESS` | `RESOLVED` | Allowed |
| `IN_PROGRESS` | `CANCELLED` | Allowed |
| `RESOLVED` | `CLOSED` | Allowed |

## Invalid Transitions

Any transition not listed in the valid-transition table is invalid, including same-state requests. `CLOSED` and `CANCELLED` are terminal states and have no outgoing transitions. Examples include:

| Current state | Requested state | Result |
| --- | --- | --- |
| `CLOSED` | `OPEN` | Rejected |
| `CLOSED` | `IN_PROGRESS` | Rejected |
| `RESOLVED` | `OPEN` | Rejected |
| `RESOLVED` | `CANCELLED` | Rejected |
| `CANCELLED` | `OPEN` | Rejected |
| `CANCELLED` | `IN_PROGRESS` | Rejected |
| Any state | Same state | Rejected |

## Transition Rules

- A ticket may move only along a valid transition listed above.
- Every new ticket starts in `OPEN`.
- The backend must validate each requested transition; frontend restrictions do not replace backend validation.
- Request a status transition using `PATCH /api/tickets/{id}/status` with a JSON body containing the target status, for example `{ "status": "IN_PROGRESS" }`.
- A rejected transition must not change the ticket's current state.
- Valid transitions have no additional business preconditions or required side effects.

## Expected Backend Behavior for Invalid Transitions

- Reject the requested transition and leave the ticket in its current state.
- Return HTTP `409 CONFLICT`.
- Return the error using the project's standard API error format.
- Do not expose internal exception details.