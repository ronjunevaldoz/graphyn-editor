# Plans

This folder is for phased execution notes and task breakdowns.

## Phase Checklist

### Phase 1: Foundation

- [x] Create the KMM baseline with Android, Desktop, Web, and Server entrypoints.
- [x] Define the core workflow model, types, and serialization layer.
- [x] Add the runtime plugin API for node specs and executors.
- [x] Add the editor plugin API for custom panels.
- [x] Add shared demo bootstrap helpers.

### Phase 2: Demo Interaction

- [x] Preload a sample workflow so the editor opens with content.
- [x] Render registered node specs in the palette.
- [x] Support node selection and node movement.
- [x] Support simple output-to-input connection creation.
- [x] Add node creation from the palette.
- [x] Add drag-preview feedback for new connections.

### Phase 3: Validation

- [x] Enforce port type compatibility before creating a connection.
- [x] Show validation errors in the inspector.
- [x] Prevent invalid self-loops or cycles where needed.
- [x] Surface workflow validation state in the editor shell.

### Phase 4: Editing UX

- [x] Delete selected nodes.
- [x] Delete selected connections.
- [x] Reconnect existing connections.
- [x] Improve empty states and canvas hints.
- [x] Add clearer drag/hover states for ports.

### Phase 4.6: Connection-Drop Node Picker ✓

- [x] Releasing a connection draft on empty canvas shows a floating node picker popup at the drop position.
- [x] Picker filters to only nodes compatible with the dragged port type.
- [x] Picking a node adds it at the drop world position and auto-connects it.
- [x] Dismissing the picker (click-away / Escape) cancels the draft.

### Phase 4.5: Gesture & Keyboard Polish ✓

Gaps found by comparing with the stable old Graphyn codebase.

- [x] Cancel connection draft when user releases on empty canvas (currently hangs forever).
- [x] Cancel connection draft on two-finger pinch/scroll while dragging.
- [x] Cancel connection draft on Escape key.
- [x] Delete selected node/connection on Backspace / Delete key.
- [x] Unify the three separate `pointerInput` blocks in the canvas into a single gesture coordinator (pan, node drag, connection drag — one `ActiveDrag` state, one modifier).
- [x] Start connections from input ports as well as output ports (drag backward from input → find compatible output).

### Phase 5: Plugin Ecosystem

- [ ] Document the external plugin module layout.
- [ ] Add a sample third-party plugin example.
- [ ] Define a simple plugin packaging and distribution story.
- [ ] Decide whether discovery stays explicit only or later supports registry loading.

### Phase 6: Server Runtime

- [ ] Wire the server host to the runtime plugin registry.
- [ ] Add a workflow execution endpoint.
- [ ] Serialize workflow input/output payloads for server use.
- [ ] Keep execution synchronous for the MVP.

Tasking style:
- Keep tasks small and outcome-based.
- Reference architecture docs instead of duplicating rules here.
- Update the plan when a phase changes, not for every tiny implementation detail.

Current focus: Phase 5 — plugin ecosystem.

Implementation notes:
- Phase 2–4.5 complete. Port dots at card edges (n8n-style), reconnect via midpoint click, hover states on ports, gesture/keyboard polish.
- Phase 4.5: empty-canvas click/Escape/Backspace cancel drafts; bidirectional port connections (input→output); scroll/pinch cancels draft; separate gesture modifiers per concern.
- Phase 5 should build on the now-stable editor to showcase the plugin API.
