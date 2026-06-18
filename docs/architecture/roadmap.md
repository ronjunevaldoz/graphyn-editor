# Implementation Roadmap

Items discovered during development that are not yet built. Ordered by impact.

---

## UI — FieldCard missing row types

| Gap | File(s) | Work |
|---|---|---|
| `ListType` port has no array UI | `ui/cards/FieldCardList.kt` (new), `FieldCardHelpers.kt` | New `ListRow` composable; add/remove items, typed per `elementType`; route `is WorkflowType.ListType` in `FieldBody` |
| `RecordType` port falls through to text input | `ui/cards/FieldCardRecord.kt` (new) | Expandable sub-field rows, one row per field key |
| `NullableType` has no null toggle | `ui/cards/FieldCardHelpers.kt` | Checkbox + optional value widget beside it |

---

## Core — execution gaps

| Gap | File(s) | Work |
|---|---|---|
| Cycle detection throws but is never surfaced to UI | `editor/state/GraphynEditorStateDispatch.kt` | Catch `WorkflowExecutionException`, dispatch error state or log entry |
| `OpaqueType` has no compatibility rule | `core/model/WorkflowTypeCompatibility.kt` | Add explicit passthrough case |
| No streaming/partial execution | `core/execution/WorkflowExecutionEngine.kt` | Optional: emit per-node progress via `Flow` |

---

## Editor UX

| Gap | File(s) | Work |
|---|---|---|
| Type mismatch gives no explanation | `editor/canvas/GraphynPortLayer.kt` | Show tooltip or inspector message: "expected X, got Y" |
| No port tooltip on hover | `editor/canvas/GraphynPortLayer.kt` | `Popup` on port dot hover showing name + type |
| Auto-layout skip is silent | `editor/state/GraphynAutoLayout.kt` | Add log entry when skipped due to `> MAX_NODES` |

---

## Status

- [ ] `ListRow` — **next up**
- [ ] `RecordRow`
- [ ] `NullableType` toggle
- [ ] Cycle error surface
- [ ] `OpaqueType` compatibility
- [ ] Type mismatch tooltip
- [ ] Port hover tooltip
- [ ] Auto-layout skip log
