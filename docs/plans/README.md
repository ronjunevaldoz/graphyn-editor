# Plans

This folder is for phased execution notes and task breakdowns.

Current approach:
1. Define the library boundaries.
2. Lock the core data model and type system.
3. Implement node registration and execution.
4. Build the editor shell and custom panel hosting.
5. Add workflow authoring and validation flows.

Tasking style:
- Keep tasks small and outcome-based.
- Reference architecture docs instead of duplicating rules here.
- Update the plan when a phase changes, not for every tiny implementation detail.

Phase 2 focus:
- Add a reusable canvas surface for workflow nodes.
- Keep node placement and selection in editor state.
- Render node metadata from registered specs, not hardcoded UI.
- Keep output/state changes reactive so the canvas and inspector update together.
