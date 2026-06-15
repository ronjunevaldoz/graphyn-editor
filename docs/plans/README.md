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
- [ ] Add node creation from the palette.
- [ ] Add drag-preview feedback for new connections.

### Phase 3: Validation

- [ ] Enforce port type compatibility before creating a connection.
- [ ] Show validation errors in the inspector.
- [ ] Prevent invalid self-loops or cycles where needed.
- [ ] Surface workflow validation state in the editor shell.

### Phase 4: Editing UX

- [ ] Delete selected nodes.
- [ ] Delete selected connections.
- [ ] Reconnect existing connections.
- [ ] Improve empty states and canvas hints.
- [ ] Add clearer drag/hover states for ports.

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

Current focus:
- Add node creation from the palette.
- Add connection preview feedback.
- Keep node placement and selection in editor state.
- Keep output/state changes reactive so the canvas and inspector update together.

Implementation note:
- We already have a working sample graph and simple port-based connections.
- The next visible editor win is adding palette-driven node creation so users can build graphs without editing the seed workflow.
