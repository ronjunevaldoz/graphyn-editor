# Graphyn — Claude Code Guidance

## Project

Kotlin Multiplatform (KMP) library-first workflow editor. Targets: Android, Desktop (JVM), Web (JS/WASM), iOS, Server.

- `core/` — pure Kotlin types, no Compose dependency
- `editor-api/` — editor plugin contracts
- `plugin-api/` — node plugin contracts
- `app/shared/` — Compose Multiplatform UI (canvas, shell, state)
- `plugins/` — sample first-party plugins
- `docs/` — architecture notes, plans, test coverage

## File-size rule

**Keep every file under 150 lines.** If a file exceeds that, split it before adding more. This is a hard ceiling, not a suggestion.

Preferred split patterns:
- One public composable per file (extract private helpers into a sibling `*Helpers.kt` or a sub-component file)
- State classes: extract each logical concern into its own file (`GraphynEditorNodeState`, `GraphynEditorConnectionState`, `GraphynEditorViewportState`) and compose them in the top-level state
- Long `when` dispatch → move each branch into a dedicated handler object

Files currently over the limit are tracked in `docs/architecture/code-audit.md`.

## Kotlin / Compose conventions

- MVI: all mutations go through `dispatch(GraphynEditorIntent)` on `GraphynEditorState`
- Port IDs are plain names (`"message"`), not `"name:type"` strings
- Canvas positions are dp-as-int (`IntOffset`); convert to px inside `Modifier.offset { }` or `DrawScope` lambdas using `.dp.roundToPx()`
- Previews live in `jvmMain`, import `androidx.compose.ui.tooling.preview.Preview` (not the `org.jetbrains` alias)
- No comments unless the WHY is non-obvious

## style-nodes plugin

`plugins/style-nodes` contains exactly **3 node specs** — one per card style — and must stay that way:

| Spec | Card | Purpose |
|---|---|---|
| `kSampler` | `DarkHeaderCard` | Multi-port, coloured header |
| `distributePoints` | `FieldCard` | Labelled field rows |
| `webhook` | `CircleCard` | Compact trigger/sink |

**Do not add domain-specific nodes here.** If a demo needs more nodes, define them as local `WorkflowDefinition` data in `app/demo` — not as registered plugin specs. The plugin's job is to demonstrate card shapes, not to model real workflows.

## Documenting learnings

When a session uncovers something non-obvious — a hidden constraint, a tricky bug root cause, a Kotlin/Compose gotcha, a module dependency surprise — write it up in `docs/architecture/lessons.md` before finishing. One bullet per finding: what the problem was, why it happened, and what the fix or rule is. This prevents the same issue from being rediscovered in future sessions.

## Testing

- Common unit tests → `commonTest`
- JVM UI / screenshot tests → `jvmTest` using `createComposeRule()` and Roborazzi
- Every interactive canvas element must have a `testTag` so UI tests can address it
- Run: `./gradlew :app:shared:jvmTest`
- Docs: `docs/architecture/test-coverage.md`
