# Review Project Specifications

Review all specification files in the repository as a coherent contract. Do not implement code or modify the specifications during this review.

Identify:

- Contradictions within a specification or between specifications.
- Missing functional, non-functional, security, or error-handling requirements that block consistent implementation or verification.
- Ambiguous terms, unspecified edge cases, and assumptions that different implementers could interpret differently.
- Inconsistent API paths, methods, request or response fields, status codes, validation, and error formats.
- Inconsistent state-machine rules, including states, allowed and forbidden transitions, transition conditions, and resulting side effects.

Report each issue with the relevant specification file and section, explain why it is unclear or inconsistent, and propose a focused clarification as a question or suggested decision. Prioritize blockers and cross-spec inconsistencies. Do not silently choose a behavior or invent a requirement. If no issues are found, say so and note any areas that remain unspecified.