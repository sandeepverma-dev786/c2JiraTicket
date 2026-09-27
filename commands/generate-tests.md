# Generate Tests From Specifications

Use the approved specifications and current implementation to identify and generate missing tests. Treat the specifications as the source of expected behavior; use the implementation to locate test targets, not to redefine the contract.

- Inspect the repository's existing test framework, style, fixtures, and test commands before writing tests.
- Cover specified behavior that is not already adequately tested, including business rules, boundary cases, request validation, and allowed and rejected state transitions where applicable.
- Create or update test files only. Do not change production code, specifications, build configuration, or dependencies.
- Do not encode behavior that is absent from or ambiguous in the approved specifications. Report the ambiguity or any mismatch between specification and implementation instead of guessing.
- Keep tests deterministic and focused, with meaningful names and assertions on observable behavior. Avoid duplicating adequate coverage.
- Run the narrowest relevant test command available and report the tests added, results, and any blocked or uncovered requirements.