# Agent Rules

This file stays thin on purpose.

Core rules:
- Prefer the repo docs over memory when making structural decisions.
- Keep the library core UI-agnostic.
- Keep editor-only concerns out of the core model.
- Add new guidance by reference, not by growing this file into a handbook.

Reference docs:
- [Project Structure](./architecture/README.md)
- [Plan Phases](./plans/README.md)
- [Type Model](./architecture/types.md)
- [Skills Prospect](./skills/README.md)

Working rule:
- If a decision changes architecture, add or update a referenced doc first, then implement code.
