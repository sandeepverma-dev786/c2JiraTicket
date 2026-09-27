# Testing Guidelines

Derive expected behavior from approved specifications and existing project conventions. Do not invent business rules or state transitions to make a test pass.

## Test Scope

- Use unit tests for isolated business logic and decision-making. Replace external collaborators only where isolation is useful; do not mock the behavior under test.
- Use integration tests for important framework, persistence, serialization, and component-boundary behavior. Keep them focused on observable contracts.
- Test business rules through meaningful inputs and observable outcomes, including boundaries, invalid cases, and relevant combinations.
- For stateful workflows, cover each specified state transition, rejected transition, and resulting state or side effect. Use only states and transitions defined by the approved specification.
- Test request validation for required values, format and range constraints, and cross-field rules where specified. Confirm invalid requests produce the documented error response.

## Test Quality

- Name tests for the behavior and expected outcome, for example `rejectsTransitionWhenTicketIsClosed`.
- Keep each test focused, readable, and independent. Assert outcomes and important side effects rather than implementation details.
- Make tests deterministic: control time, randomness, generated identifiers, and external responses when they affect outcomes. Do not depend on execution order, real network services, arbitrary sleeps, or shared mutable fixtures.
- Use representative fixtures and clean up test data. Avoid over-mocking, broad assertions, and tests that pass without proving a requirement.
- Keep unit and integration test responsibilities distinct, and use the repository's existing test libraries and execution conventions.