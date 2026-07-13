# Graphyn — Claude Code Guidance

## Project

Kotlin Multiplatform (KMP) library-first workflow editor. Targets: Android, Desktop (JVM), Web (JS/WASM), iOS, Server.

- `core/` — folder only (no `:core` module), pure-Kotlin, no Compose. Consumers depend on the specific submodule(s) they use:
  - `core:model` — workflow graph/types/values, registry, validation, sync, `NodeGroups` (palette folder names)
  - `core:execution` — execution engine, executors, events (depends on `core:model`); also hosts the full-stack `CoreWorkflowTest` + `benchmarkCore`
  - `core:serialization` — workflow document codec (depends on `core:model`)
  - `core:data` — workflow stores + platform persistence actuals (depends on `core:model`)
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

## Plugin modules

Plugin modules (`plugins/*`) are **library modules**, not sample galleries:

- Each plugin contains only its production runtime and editor implementation
- **Never** add demo workflows, placeholder node specs, or test data to any plugin module
- If a demo or test needs illustrative nodes, define them as local `WorkflowDefinition` data in `app/demo`
- Plugin specs that exist solely to show a UI shape are fine — but the spec must model a real, named operation, not generic scaffolding

## style-nodes plugin

`plugins/sample-style-nodes` contains exactly **3 node specs** — one per card style — and must stay that way:

| Spec | Card | Purpose |
|---|---|---|
| `kSampler` | `ShapeCard` | Multi-port, compact shape card |
| `distributePoints` | `FieldCard` | Labelled field rows |
| `webhook` | `CircleCard` | Compact trigger/sink |

**Do not add domain-specific nodes here.** If a demo needs more nodes, define them as local `WorkflowDefinition` data in `app/demo` — not as registered plugin specs. The plugin's job is to demonstrate card shapes, not to model real workflows.

## KDoc

All public API surfaces in `core/`, `editor-api/`, `plugin-api/`, and `ui/cards/` must have KDoc. This includes classes, interfaces, functions, and properties that a plugin or app author would consume.

- Prefer KDoc over separate markdown docs — IDEs surface it inline, it stays co-located with the code, and it can't go stale independently
- Delete any `docs/architecture/*.md` file whose content belongs on the code it describes
- KDoc must explain the **why** or the **contract**, not just restate the name — one sentence is enough if that's all it takes
- Include usage examples (` ```kotlin ` blocks) when the call site isn't obvious from the signature

## Maven publishing

All publishing config is centralized in the **`graphyn-maven-publish` convention plugin**
(`build-logic/src/main/kotlin/graphyn-maven-publish.gradle.kts`). It owns `automaticRelease = true`,
the signing guard, group/version, and the shared license/developer/scm POM. **Do not hand-copy a
`mavenPublishing { }` block** — that duplication is what caused the same bug to ship 11 times.

### Published artifacts

| Module | Artifact ID |
|---|---|
| `:core:model` | `graphyn-core-model` |
| `:core:execution` | `graphyn-core-execution` |
| `:core:serialization` | `graphyn-core-serialization` |
| `:core:data` | `graphyn-core-data` |
| `:plugin-api` | `graphyn-plugin-api` |
| `:ai` | `graphyn-ai` |
| `:editor-api` | `graphyn-editor-api` |
| `:runtime` | `graphyn-runtime` |
| `:ui:cards` | `graphyn-ui-cards` |
| `:app:shared` | `graphyn-editor` |
| `:server` | `graphyn-ktor-plugin` |

### Rules

- **To make a module publishable:** apply `id("graphyn-maven-publish")` in its `plugins {}` block (instead of the `dokka` + `mavenPublish` aliases), then add a minimal `mavenPublishing { coordinates(artifactId = "graphyn-…"); pom { name.set(…); description.set(…) } }`. Everything else (automaticRelease, signing, group, version, license/scm) comes from the convention plugin. The `:server` module is the one exception — it sets its own `project.group` for the application jar, so it passes the Maven groupId explicitly: `coordinates("io.github.ronjunevaldoz", "graphyn-ktor-plugin", libraryVersion)`.
- **`api()` deps in published modules must themselves be published.** If a project dep would appear in the POM but isn't on Maven Central, change it to `implementation()`. Source-only modules (`plugins/*`, `core:designsystem`) must never be `api()` deps of a published module. The `verifyPublishing` task enforces this.
- **When adding a new published module:** add it to the `publishedModulePaths` set in `build.gradle.kts`, the `ARTIFACTS` array in `scripts/verify-maven-central.sh`, `publish.yml` (in dependency order), and the `PUBLISH_GROUPS` array in `scripts/publish-local.sh`.
- **Guardrails (don't bypass them):**
  - `./gradlew verifyPublishing` (runs in `ci.yml` on every PR) fails if a published module doesn't apply the convention plugin or has an `api()` leak.
  - `scripts/verify-maven-central.sh` (runs in `publish.yml` after publishing) polls `repo1.maven.org` and fails the release if any artifact didn't land — a green publish job means *actually published*.
  - Local manual publish: `scripts/publish-local.sh <version>` (uses Doppler for credentials).
- Reference: `docs/reference/compatibility-matrix.md` tracks all published artifacts and their first-available version.

## Documenting learnings

When a session uncovers something non-obvious — a hidden constraint, a tricky bug root cause, a Kotlin/Compose gotcha, a module dependency surprise — write it up in `docs/architecture/lessons.md` before finishing. One bullet per finding: what the problem was, why it happened, and what the fix or rule is. This prevents the same issue from being rediscovered in future sessions.

## Testing

- Common unit tests → `commonTest`
- JVM UI / screenshot tests → `jvmTest` using `createComposeRule()` and Roborazzi
- Every interactive canvas element must have a `testTag` so UI tests can address it
- Run: `./gradlew :app:shared:jvmTest`
- Docs: `docs/architecture/test-coverage.md`
