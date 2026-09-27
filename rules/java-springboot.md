# Java 21 and Spring Boot Guidelines

Use Java 21 language and standard-library features where they improve clarity. Follow the Java and Spring Boot versions, dependencies, and conventions established by the repository; do not upgrade or introduce dependencies without a requirement.

## Architecture

- Keep responsibilities separated: controllers handle HTTP concerns, services coordinate use cases, and repositories handle persistence. Keep business rules in an appropriate domain or service layer rather than in controllers or persistence code.
- Make dependencies explicit with constructor injection. Avoid field injection and service-locator patterns.
- Keep boundaries small and cohesive. Add an abstraction only when it expresses a real domain concept, isolates a meaningful dependency, or removes substantial duplication.

## Data and Validation

- Use request and response DTOs at API boundaries. Do not expose persistence entities as API contracts.
- Validate untrusted input at the boundary with Jakarta Bean Validation where appropriate, and enforce important invariants in the business layer as well.
- Use explicit mapping between API models and domain or persistence models. Keep mapping straightforward and avoid generic mapping frameworks unless the project already uses one for good reason.

## Errors and Maintainability

- Translate expected domain and persistence failures into deliberate application errors. Handle HTTP translation centrally rather than duplicating it in controllers.
- Prefer clear names, focused methods, immutable values where practical, and straightforward control flow. Avoid speculative features, redundant wrappers, and abstractions that obscure behavior.
- Keep configuration externalized. Avoid hidden mutable global state and catch exceptions only when the code can handle or meaningfully translate them.

## Security

- Apply authentication and authorization according to the approved requirements; enforce access checks on every relevant operation, including object-level ownership or access rules.
- Treat all client input as untrusted. Avoid mass assignment, unsafe deserialization, injection-prone queries, and leaking secrets or sensitive ticket/customer data.
- Store secrets outside source control and use secure defaults for transport, cookies, and security headers as applicable. Do not log credentials, tokens, or unnecessary personal data.