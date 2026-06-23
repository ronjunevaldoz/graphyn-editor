# Implementation Roadmap

Items discovered during development that are not yet built. Ordered by impact.

---

## UI — FieldCard missing row types

| Gap | File(s) | Status |
|---|---|---|
| `ListType` port row | `ui/cards/FieldCardList.kt` | ✅ done |
| `RecordType` port row | `ui/cards/FieldCardRecord.kt` | ✅ done |
| `NullableType` null toggle | `ui/cards/FieldCardHelpers.kt` | ✅ done |

---

## Core — execution gaps

| Gap | File(s) | Status |
|---|---|---|
| `OpaqueType` compatibility rule | `core/model/WorkflowTypeCompatibility.kt` | ✅ done |
| Cycle detection never surfaced to UI | `editor/state/GraphynEditorStateDispatch.kt` | ✅ done (catch in execution actions, all nodes → Error) |
| No streaming/partial execution | `core/execution/WorkflowExecutionEngine.kt` | ⬜ optional: per-node progress via `Flow` |

---

## Editor UX

| Gap | File(s) | Status |
|---|---|---|
| Type mismatch gives no explanation | `editor/canvas/GraphynPortLayer.kt` | ✅ done (toast bottom-center + `rejectedConnectionPort` log) |
| No port tooltip on hover | `editor/canvas/GraphynPortLayer.kt` | ✅ done (`PortTooltip` Popup on input/output dot hover) |
| Auto-layout skip is silent | `editor/state/GraphynAutoLayout.kt` | ✅ done (log entry when `> MAX_NODES`) |

---

## Canvas features

| Feature | Status |
|---|---|
| Sticky notes | ✅ done (`plugins/sticky-notes`) |
| Group nodes (visual frame around selection) | ✅ done (`NodeGroup`, `GraphynGroupLayer`, Cmd+G) |
| Subgraphs — execution (recursive, parallel) | ✅ done (`WorkflowExecutionEngine`) |
| Subgraphs — input injection (parent → inner free ports) | ✅ done (`buildInputMap` `externalInputs`) |
| Subgraphs — drill-in navigation + breadcrumb | ✅ done (`GraphynSubgraphNavigator`) |
| Subgraphs — collapse selection (Cmd+Shift+G) / expand (inspector) | ✅ done (`collapseToSubgraph`/`expandSubgraph`, derived boundary specs) |

---

## Plugin loading

| Feature | Status |
|---|---|
| Explicit host registration | ✅ done (`GraphynPluginRegistry.install`) |
| JVM/Android `ServiceLoader` auto-discovery | ✅ done (`discoverGraphynPlugins`, `installDiscovered`) |

---

## First-party plugins

| Plugin | Purpose | Status |
|---|---|---|
| `plugins/list-ops` | map, filter, reduce, zip | ✅ done |
| `plugins/control` | branch, merge, loop | ✅ done |
| `plugins/types` | cast, validate, schema | ✅ done |
| `plugins/text` | format, split, regex | ✅ done |
| `plugins/io` | http-request, file-read, file-write | ✅ done |
