# Review Implementation Against Specifications

Review the current implementation against all approved specifications and repository guidance. Do not modify or generate code or edit files during this review.

Check for:

- Bugs, incorrect behavior, and edge cases.
- Missing, incomplete, or conflicting requirement coverage.
- Incorrect assumptions where a specification is silent or ambiguous.
- Security and privacy issues, including authorization gaps and unsafe data handling.
- Unnecessary complexity, duplication, or abstractions that make the implementation harder to maintain.
- Missing or weak tests, especially for business rules, validation, and specified state transitions.

Report actionable findings first, ordered by severity. For each finding, include its severity, affected file and location, the violated requirement or risk, and a concise explanation. Separate confirmed findings from questions or assumptions. If no findings are identified, say so and note any relevant review limitations. Do not make changes as part of the review.