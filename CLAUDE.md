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

## Testing

- Common unit tests → `commonTest`
- JVM UI / screenshot tests → `jvmTest` using `createComposeRule()` and Roborazzi
- Every interactive canvas element must have a `testTag` so UI tests can address it
- Run: `./gradlew :app:shared:jvmTest`
- Docs: `docs/architecture/test-coverage.md`
