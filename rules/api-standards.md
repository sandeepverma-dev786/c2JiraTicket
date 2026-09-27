# API Standards

Treat approved specifications as the source of truth for API behavior. Record contract decisions there before implementation when a convention is not yet specified.

## REST and Naming

- Model endpoints as resource-oriented paths, using consistent plural nouns for collections and stable identifiers for individual resources. Avoid verbs in paths unless an explicitly modeled action cannot be represented clearly as a resource operation.
- Use consistent casing and nesting. Keep nesting shallow and do not expose database table names or internal implementation details.
- Use JSON consistently for request and response bodies unless the contract requires another media type. Represent dates and times in a documented, unambiguous format.

## HTTP Methods and Status Codes

- Use `GET` to read, `POST` to create or invoke a documented non-idempotent operation, `PUT` to replace, `PATCH` to partially update, and `DELETE` to remove a resource.
- Preserve HTTP method safety and idempotency semantics. Document concurrency behavior and repeat-request behavior where relevant.
- Use status codes consistently: `200` for a successful response with a body, `201` for resource creation, `204` for success without a body, `400` for malformed or invalid requests, `401` for missing or invalid authentication, `403` for insufficient permission, `404` for unavailable resources, and `409` for conflicts with current state. Use other codes only when the contract defines their meaning.

## Validation and Errors

- Validate request shape and values at the API boundary, and return client errors in the documented format. Do not rely on clients to enforce server-side rules.
- Use one consistent error-response structure across endpoints. Include a stable machine-readable error identifier and safe message; include field-level details when useful and specified. Do not expose stack traces, SQL, internal class names, secrets, or sensitive data.
- Map expected validation, authorization, not-found, and conflict failures deliberately. Unexpected exceptions should produce a generic server error and be logged safely for diagnosis.

## Contract Consistency

- Keep request and response field names, pagination, filtering, sorting, and error semantics consistent across related endpoints.
- Document authentication and authorization requirements, required headers, and any compatibility or versioning policy in the API specification.