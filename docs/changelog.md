# Changelog

## 0.1.0 — 2026-06-18

Initial public release.

### Features
- Canvas with pan, zoom, drag, and port-to-port connections
- MVI pattern: `dispatch(GraphynEditorIntent)` for all mutations
- Undo / redo (Cmd+Z / Cmd+Shift+Z) with 50-entry snapshot history
- Multi-select, group move, copy, paste, duplicate (Cmd+A/C/V/D)
- Connection type validation via `WorkflowTypeCompatibility`
- Node execution state badges (Running / Success / Error)
- Palette search filter
- Workflow JSON serialization (`toJson()` / `workflowFromJson()`)
- Plugin registry for nodes and editor panels
- `@GraphynExperimentalApi` annotation for unstable `Default*` implementations
- Maven Central artifacts: `graphyn-core`, `graphyn-plugin-api`, `graphyn-editor-api`, `graphyn-editor`
- iOS XCFramework via `assembleGraphynEditorXCFramework`
