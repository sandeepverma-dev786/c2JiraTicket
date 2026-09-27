---
name: documentation
description: "Use when creating, updating, or reviewing project documentation to keep it accurate, consistent, and aligned with the implementation and approved specifications."
---

# Project Documentation

Maintain documentation as part of the project contract, not as a separate description of intended behavior.

- Treat approved specifications as authoritative for required behavior. Treat the implementation as authoritative for what currently exists; clearly distinguish implemented behavior from planned behavior.
- When implementation changes affect documented behavior, update the relevant documentation in the same change. Do not claim a feature, API, configuration option, or guarantee exists unless verified in the implementation or approved specification.
- Keep API examples, terminology, state and transition descriptions, error behavior, setup instructions, and configuration details consistent across documents.
- Prefer concise, task-oriented content with concrete examples where they clarify a contract. Link to a single source of truth instead of copying large sections that can drift.
- Preserve unresolved questions and assumptions explicitly. Do not resolve specification gaps by inventing behavior; request clarification or mark the documentation as incomplete.
- Review nearby documentation when changing a contract, and remove or revise statements that have become inaccurate. Do not rewrite unrelated material.