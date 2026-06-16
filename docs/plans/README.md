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
- Delete selected nodes.
- Delete selected connections.
- Reconnect existing connections.
- Improve empty states and canvas hints.

Implementation note:
- Phase 2 is now functionally complete in the current shell.
- Phase 3 validation is also wired into the editor shell and core validator.
- The next visible editor win is editing UX polish: delete, reconnect, and clearer canvas hints.
