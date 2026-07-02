# Engineering Lessons

General patterns discovered during development. Each entry is written to be reusable
across projects and to serve as raw material for agent skills.

---

## Settings keys should be canonical snake_case with legacy aliases

**Category:** Editor settings migration

When the credentials dialog only edits a subset of rows or stores legacy `GRAPHYN_*` keys
verbatim, users end up with a split-brain config: some values are editable in the UI, while
others are effectively frozen under old names.

**Rule:** Store the editor-facing settings keys in snake_case, let every built-in row edit both
key and value, and keep a compatibility layer that reads legacy keys and normalizes them on save.

## OpenAPI should decide the cancel path shape

**Category:** Stable Diffusion worker contract

The live `server-sd` spec exposes both `/api/sd/cancel` and `/api/sd/cancel/{jobId}`. Posting a
JSON body to the generic route works only if the worker tolerates that guess; the documented path
variant is the safer contract for specific job cancellation.

**Rule:** When the worker publishes OpenAPI, align the client to the documented path and method
first, then keep a compatibility fallback only if the server version in the wild still needs it.

## Stable Diffusion workers should be selected by settings, not by hardcoded hostnames

**Category:** Editor integration — configurable inference backends

When the editor or backend code hardcodes a deployment hostname, local dev and hosted
RunPod workers drift into separate code paths. That makes readiness, status, job polling, and
cancelation checks harder to keep aligned, and it hides the real contract behind the worker URL.

**Rule:** Keep the worker adapter behind a single HTTP client that resolves `baseUrl` + API key
from the active environment on every request. Local and RunPod profiles should differ only by
URL and auth key; the workflow graph must stay unchanged.

## Launcher catalogs need an explicit priority rule once badges and recency matter

**Category:** Workflow launcher ordering

When the launcher relied on enum declaration order alone, the media catalog looked random as
new templates were appended and badge-highlighted entries drifted below older ones. That made the
catalog feel stale even though the underlying workflows were current.

**Rule:** Sort launcher catalogs explicitly by section, then by badge priority, then by recency
within the section. Use enum order only as the recency signal, not as the whole layout policy.

## Stitching requires video clips, not stills

**Category:** Media workflow composition

`media.video_stitch` only concatenates compatible video handles. It cannot take a stack of still
images or raw caption data directly, so a shorts pipeline needs to generate or render each scene
into its own clip first.

**Rule:** Generate per-scene motion clips before stitching, then apply caption burn-in to the final
stitched video unless the caption timing depends on the combined runtime.

## Nullable fields should round-trip as `NullValue`, not empty strings

**Category:** Workflow value mapping — media caption styles

When a record field is modeled as nullable, serializing `null` to `StringValue("")` creates a
false value that downstream mappers may still try to parse as real data. In the media caption
style path, that turns "no background color" into an invalid color string and breaks the
renderer.

**Rule:** Preserve `WorkflowValue.NullValue` for absent nullable fields. Only emit empty strings
when the contract truly means "present but blank."

---

## Script-based media templates need the script plugin in the JVM runtime bundle

**Category:** Desktop launcher / catalog visibility

Any launcher template that uses `script.eval` will be filtered out unless the desktop runtime
installs the script plugin alongside the media plugins. That makes the template look "missing"
even when the workflow definition itself is valid.

**Rule:** If a first-party desktop template depends on `script.eval`, add `ScriptPlugin` to the JVM
runtime bundle and the corresponding Gradle source-set dependency so the template is visible and
compiles together with the host.

---

## Subgraph boundary ports are not automatically understood by the structural validator

**Category:** Workflow validation

The execution engine can inject outer subgraph inputs into inner free ports by name, but the
structural validator still sees the registered subgraph node shape. That means intentional
connections into derived subgraph ports can surface as `missing_input_port` during graph
validation.

**Rule:** When a template deliberately uses subgraph boundary injection, either keep the validator
exception scoped to that template family or model the boundary with an explicitly registered node
spec that advertises the same ports.

**Follow-up:** Generic pass-through subgraphs are still useful for demos, but a reusable template
that fans out prompts or fans in multiple clips should get a dedicated node spec with matching
outer ports. That keeps validation, editor wiring, and runtime execution aligned.

---

## KMP `implementation()` deps still leak into the published POM

**Category:** Maven publishing — Kotlin Multiplatform

In standard JVM Gradle, `implementation()` keeps a dep off the POM. In KMP it does not — `implementation()` generates a `runtime`-scoped coordinate in every published POM, same as `api()` generates `compile`-scoped. Any project dependency (published or not) on a published KMP module will appear in the POM with its Gradle project coordinates (`Graphyn.plugins:sample-logger:unspecified`) which Maven consumers cannot resolve.

**Rule:** Every project dep in a published KMP module — whether `api()` or `implementation()` — must itself be published to Maven Central. The audit task must check both configuration types, not just `api`.

**Fix applied:** Extended `verifyPublishing` check 2 to scan `*MainImplementation` configurations in addition to `*MainApi`.

---

## Reverse-check pattern for convention plugin enrollment

**Category:** Gradle audit tasks — publish guardrails

A `verifyPublishing` task that only checks "listed modules are correctly wired" misses the inverse: a module that applies the publish convention plugin but is absent from the published set. New plugins are silently skipped at release time.

**Rule:** The audit must enforce both directions — listed modules apply the plugin AND modules applying the plugin are listed. Applying the convention plugin is the enrollment contract; the audit enforces it symmetrically.

**Fix applied:** Added check 1b to `verifyPublishing`: scans all subprojects for `com.vanniktech.maven.publish` and fails if any aren't in the published set.

---

## `MavenPublishBaseExtension.coordinates` is a setter, not a readable property

**Category:** Gradle plugin API — vanniktech maven-publish

`MavenPublishBaseExtension.coordinates(artifactId)` is a configuration method, not a readable property. Attempting `extension.coordinates.artifactId` produces a compile error (`Function invocation expected`). There is no public getter for the stored coordinates.

**Workaround:** Read artifact IDs from `PublishingExtension.publications.withType(MavenPublication)`. Find the root publication by filtering out platform-suffixed artifact IDs (`-jvm`, `-android`, `-js`, `-wasmjs`, `-iosarm64`, `-iossimulatorarm64`, `-metadata`). For JVM-only modules the single publication has the base artifact ID directly.

---

## Maven Central deployment conflicts on partial publish retry

**Category:** Maven Central — Central Portal

If a publish run fails partway through (e.g. a compile error mid-batch), the partially uploaded deployment is left in `PUBLISHING` or `VALIDATING` state on Central Portal. Retrying the same version fails with: `Component with coordinate '...' is currently being published in another deployment`.

**Rule:** Either drop the stuck deployment at central.sonatype.com before retrying the same version, or bump the patch version. Do not retry the same version without clearing the prior deployment first.

---

## The Write-from-Fallback Trap

**Category:** State management — mutable maps with lazy initialization  
**Applies to:** Compose, KMP, any system where a map is the authoritative store for
incrementally updated values

### Pattern

A map has a safe read-fallback:

```kotlin
fun readPosition(id: String, index: Int): IntOffset =
    map[id] ?: fallbackPosition(index)   // safe — callers get a sensible value
```

A write path also has a fallback, but for the *wrong reason*:

```kotlin
fun moveBy(id: String, delta: IntOffset) {
    val current = map[id] ?: IntOffset.Zero   // ← different fallback!
    map[id] = current + delta
}
```

The map is only populated when the user performs an explicit action (e.g., adding an
item through the UI). Items that arrive from *initial state* (constructor, deserialization,
server sync) are never seeded. The first write computes from `Zero` instead of the actual
starting position, producing a sudden jump or incorrect result.

### Why it's hard to spot

- The read path and the UI both look correct — the initial values display fine because
  the read fallback kicks in.
- The write path also looks correct in isolation — the fallback is a reasonable default.
- The bug only fires on the *first mutation* of an unseen key, and only for data that
  was present at startup (not data added interactively).
- Interactive additions (e.g., adding an item through the palette) always call the
  setter before the write path runs, so they never hit the bug.

### Fix

Seed the map at init time for all entries that exist at construction:

```kotlin
init {
    initialItems.forEachIndexed { index, item ->
        map[item.id] = fallbackPosition(index)   // same fallback as the read path
    }
}
```

### Rule

**Read-fallbacks are safe. Write-from-fallback is not.**

Any time a write path computes `current + delta` (or any transformation that depends on
the prior value), the key *must* be in the map before the first write. If it might not
be, seed it at init.

Checklist:
- [ ] Does the write path have `map[key] ?: someDefault`?
- [ ] Can that key be absent at the time of the first write?
- [ ] Is the `someDefault` in the write path the *same* as the display fallback?
- [ ] Is the map seeded at construction for all data that is not interactively created?

### Evidence

Graphyn (2026-06-18): `GraphynNodeLayoutState.nodePositionsByNodeId`. Nodes from the
initial workflow were never seeded. `moveNode` computed new positions from `IntOffset.Zero`
instead of the correct fallback `IntOffset(304, 0)`, causing a hard jump to the canvas
left edge on the first drag.

**Fix location:** `GraphynEditorState.init` — added `layout.setNodePosition` for each
node in `initialWorkflow` before `viewportState.refresh`.

---

## Local Counter vs. Global Grid Index

**Category:** Rendering — tiled / infinite canvas grids  
**Applies to:** Any system that draws a repeating pattern over a scrollable or zoomable
surface by iterating from the first *visible* element

### Pattern

A grid is drawn by iterating from the first visible cell and incrementing a local counter:

```kotlin
var col = 0
var x = firstVisibleX
while (x <= lastVisibleX) {
    val isMajor = col % 4 == 0   // ← local counter, resets each frame
    draw(x, isMajor)
    x += spacing
    col++
}
```

When the viewport scrolls by any amount that is not a multiple of `4 × spacing`, the
first visible cell is no longer globally aligned. `col` starts at 0 again, so it marks
the wrong cells as major. The emphasis pattern (every 4th dot, every 10th line, etc.)
drifts with the viewport instead of staying fixed in world space.

### Why it's hard to spot

- The pattern looks correct at the initial scroll position (viewport offset = 0), because
  `col 0` happens to coincide with global index 0.
- It only breaks after the viewport has scrolled by a non-aligned amount — which is almost
  any real pan gesture.
- The drift is subtle on small grids (minor dots barely change) and dramatic on large
  spacing multiples (major grid lines jump by one full period).
- The bug survives code review because the loop logic is locally correct — the counter
  increments properly; the mistake is in what the counter *means*.

### Fix

Derive the emphasis flag from the cell's global world coordinate, not from the loop counter:

```kotlin
var x = firstVisibleX
while (x <= lastVisibleX) {
    val globalIndex = (x / spacing).roundToLong()   // stable across viewport changes
    val isMajor = globalIndex % 4 == 0L
    draw(x, isMajor)
    x += spacing
}
```

`roundToLong` (not `toInt`) absorbs floating-point accumulation errors from repeated
`+= spacing` additions. Use the same approach for both axes independently.

### Rule

**Any repeating visual pattern on a scrollable surface must be keyed to world coordinates,
not to a loop counter.**

Checklist:
- [ ] Does the drawing loop use a counter (`col++`, `row++`) to determine visual emphasis?
- [ ] Does that counter reset to 0 at the first *visible* element each frame?
- [ ] Is the first visible element guaranteed to always have global index 0?
- [ ] If not: replace the counter with `round(worldCoord / spacing) % period`.

### Evidence

Graphyn (2026-06-18): `GraphynCanvasBackdrop` major dot rendering. `columnIndex` and
`rowIndex` started at 0 at the leftmost/topmost visible dot each frame. After any pan
that shifted the starting column by a non-multiple of 4, minor dots became major and
major dots became minor. The bug was present since the backdrop was first written and
survived multiple refactor sessions unnoticed because it only manifests after panning.

**Fix location:** `GraphynCanvasBackdrop.kt` — replaced `columnIndex % 4` and
`rowIndex % 4` with `(worldX / worldSpacing).roundToLong() % 4` and
`(worldY / worldSpacing).roundToLong() % 4`.

---

## Pan Gesture Dead Zones from Fixed Hit Boxes

**Category:** Gesture handling — Compose pointer input  
**Applies to:** Any canvas that has nodes with varying sizes and a background pan gesture

### Pattern

A background `pointerInput` pan gesture checks whether the pointer is over a node before
activating. The check uses a fixed size (`280×180`) for every node regardless of the actual
card factory's dimensions. Custom cards that are smaller (e.g., `CircleCard` at `64×64`)
leave a dead zone: the region inside `280×180` but outside the real card accepts neither
drag (pan bails out thinking it's over the node) nor node interaction (the card isn't there).

### Why it's hard to spot

- Default-sized nodes work perfectly — the fixed `280×180` matches.
- Only non-default card sizes expose the dead zone, and CircleCard is the only one smaller
  than the default, so it was missed until multiple card styles existed.
- A misguided fix (`requireUnconsumed = true`) appears to work in isolation but fails
  because `clickable` does not consume the down `PointerInputChange` in the Main pass.

### Fix

Pass `NodeCanvasRegistry` into `graphynPanGesture` and look up each node's factory to get
its actual `nodeWidth`/`nodeHeight`. Fall back to `GraphynCanvasMetrics.NodeSize` only
when no factory is registered.

### Rule

**Hit-test bounds in gesture handlers must reflect actual rendered sizes, not a shared constant.**
Whenever a new card factory with non-default dimensions is added, no gesture code needs to
change — the registry lookup handles it automatically.

**Fix location:** `GraphynCanvasGestures.kt` — `graphynPanGesture` now accepts
`canvasCards: NodeCanvasRegistry?` and computes per-node bounds from the factory.

---

## Kotlin Enum Static Init Order

**Category:** Kotlin language — object/enum initialization  
**Applies to:** Any file where an enum class references file-level `private val`s in its constructor

### Pattern

An enum whose entries take constructor arguments referencing file-level `private val`s:

```kotlin
enum class DemoScene(val workflow: WorkflowDefinition) {
    AI(aiPipeline),      // ← references private val declared BELOW
}

private val aiPipeline = WorkflowDefinition(...)  // too late — NPE at runtime
```

The enum entries are initialized during class loading before the file-level `val`s that
follow them, resulting in a NullPointerException at runtime.

### Fix

Declare all `private val`s that enum entries reference **before** the `enum class` declaration.

### Rule

**File-level `private val`s used in enum constructors must be declared above the enum.**
Add a comment (`// Workflow definitions must be declared before the enum`) to signal this
constraint to future editors.

---

## Material3 Not Transitively Available from app/shared

**Category:** Gradle — dependency visibility  
**Applies to:** Any module that depends on `app/shared` and tries to use Material3

### Pattern

`app/shared` declares `implementation(compose.material3)` (not `api`). Any module that
depends on `app/shared` via `api(projects.app.shared)` still cannot use Material3 types,
because `implementation` dependencies are not exposed transitively.

### Fix

Use `GraphynDs.colors` + `BasicText` (from `androidx.compose.foundation.text`) instead of
`MaterialTheme` or `Text` from Material3. These are available because the Graphyn design
system is exposed via `api`.

### Rule

**Never import `androidx.compose.material3.*` in `app/app`.** If a new component needs
color or text, reach for `GraphynDs` tokens and `BasicText`.

---

## style-nodes Scope Creep

**Category:** Project design — plugin responsibility boundaries  
**Applies to:** `plugins/style-nodes`

### Pattern

`style-nodes` was built to demonstrate three card shapes (ShapeCard, FieldCard,
CircleCard). When demo scenes needed more nodes to tell a story, specs were added to the
plugin (15 total), making it look like a real domain library instead of a visual showcase.

### Rule

`style-nodes` must stay at exactly **3 specs** — one per card shape. Any additional nodes
needed for a demo live as `WorkflowDefinition` data local to `app/app`, not as registered
plugin specs. See CLAUDE.md § style-nodes plugin.

---

## Modifier.align() Requires BoxScope Receiver

**Category:** Compose — BoxScope scoping  
**Applies to:** Any standalone `@Composable` that wraps a `Box` and tries to use `Modifier.align()`

### Problem

`Modifier.align(Alignment.BottomCenter)` is an extension on `BoxScope`, not on plain `Modifier`.
A composable function extracted from a `Box` content lambda loses the `BoxScope`, so `align` is
unresolved:

```kotlin
// ❌ extracted composable — BoxScope lost
@Composable
fun Toast(msg: String) {
    Box(Modifier.align(Alignment.BottomCenter)) { ... } // Unresolved reference 'align'
}
```

### Fix

Wrap the content inside the composable in its own `Box(fillMaxSize, contentAlignment = BottomCenter)`:

```kotlin
@Composable
fun Toast(msg: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(Modifier.padding(bottom = 16.dp)...) { ... }
    }
}
```

### Rule

Only use `Modifier.align()` directly on children inside a `Box` content lambda. If you extract
that child into a named composable, add an inner `Box` to recreate the alignment context.

---

## First-Party Plugin Build File Doesn't Need Compose Unless It Uses Compose APIs

**Category:** Gradle — unnecessary dependencies  
**Applies to:** Plugin modules that only register specs and executors

### Rule

Runtime-only plugins (`GraphynPlugin`) never need `compose.runtime`, `compose.foundation`, or
`compose.ui`. Only editor plugins (`GraphynEditorPlugin`) that render custom composables need
Compose. If the build file includes Compose for a pure runtime plugin, it's over-specified.

---

## Card Visual Uniformity via Shared Color Tokens

**Category:** UI consistency — multi-card design systems

### Problem

Three card styles (`ShapeCard`, `FieldCard`, `CircleCard`) drifted independently: different
body background colors, border colors, corner radii, font sizes, and selection highlight colors.
Each card had its own hardcoded hex values with no relationship to the others.

### Solution

Extract a single `StyleNodeSharedColors.kt` file with `internal val` color constants and a
`CORNER_RADIUS` int. All three cards import from it. The only intentional differences are the
three header background colors (`DARK_HEADER_BG`, `FIELD_HEADER_BG`, `CIRCLE_BG`) — these
communicate domain at a glance and are explicitly separated in the shared file with a comment.

### Rule

When a plugin registers multiple card styles, shared surface tokens (body bg, border, selection
color, corner radius, font sizes) belong in a single shared file. Per-card identity tokens
(header accent, icon color) are documented as intentional distinctions in that same file.

---

## Annotation Nodes Need an `isAnnotation` Layering Sentinel

**Category:** Canvas rendering — z-order, minimap filtering

**Problem:** Sticky notes (and future annotation types) must always render beneath regular workflow nodes. Without a sentinel, all nodes go through a single `forEachIndexed` loop in `GraphynNodeLayer`, so placement order determines z-order — annotations placed after a regular node appear in front of it.

**Root cause:** Compose Box stacks children in composition order. A single flat render loop gives no z-separation between annotation and regular nodes.

**Fix and rule:** Add `val isAnnotation: Boolean get() = false` to `NodeCanvasFactory`. The canvas does two passes: first render all factories where `isAnnotation == true`, then render the rest. The minimap skips any factory with `isAnnotation == true`. Any future "frame" or "comment" card type should set `isAnnotation = true`.

---

## Demo-Local Plugin Pattern — Avoid Module Proliferation for Demo-Only Node Types

**Category:** Demo app architecture

**Problem:** Concepts not yet implemented (e.g., Subgraphs) still benefit from a visual demo. Creating a full plugin module for them (`plugins/subgraph/`) adds an unnecessary module and `settings.gradle.kts` entry for something that is demo-only.

**Fix and rule:** Define demo-only node types (spec + runtime plugin + editor plugin) inside `app/app/src/commonMain/kotlin/.../bootstrap/` as a single `*DemoPlugin.kt` file. Register them in `GraphynDemoPlugins.runtime` and `GraphynDemoPlugins.editor`. They live and die with the demo module. When a concept matures to production, extract it into a proper `plugins/` module.

---

## Seeding Scene-Specific Editor State with `LaunchedEffect` Inside `key()`

**Category:** Compose — state scoping, `key()` block patterns

**Problem:** When the demo tab bar switches scenes, `key(currentScene)` recreates `GraphynEditorState`. Some scenes need additional state that can't be expressed through `rememberGraphynEditorState`'s parameters (e.g., `state.groups` for the Groups scene). A `LaunchedEffect` at the outer level fires once on the first scene and never again on later scene switches.

**Fix and rule:** Put the `LaunchedEffect(Unit)` **inside** the `key(currentScene)` block, after state creation. Because `key()` disposes and restarts its content on every key change, `LaunchedEffect(Unit)` fires once per scene switch — exactly right. Gate it with `if (currentScene == DemoScene.Groups)` so only the relevant scene gets the seed. This pattern is safe because `LaunchedEffect` on the main dispatcher runs before the first composition frame is drawn.

---

## Real Subgraphs: Embed `WorkflowDefinition` on the Node, Not in Config Strings

**Category:** Node model — subgraph pattern

**Problem:** Encoding inner node names as a comma-separated config string (`"contents" → "zip, map, filter"`) is a display stub, not a real subgraph. It can't be executed, navigated into, or edited.

**Fix and rule:** Add `subgraph: WorkflowDefinition? = null` directly to `NodeRef`. The execution engine checks `node.subgraph != null` before executor lookup and runs the inner workflow recursively. The inspector reads `node.subgraph` to show count/connection info and offer an "Enter →" button. The canvas card reads `node.subgraph?.nodes` for the bullet list. No config keys required.

## Subgraph Navigation: Navigator Composable Wrapping the Shell

**Category:** Compose — drill-in navigation without a NavHost

**Problem:** `GraphynEditorShell` renders a single `GraphynEditorState`. To drill into a subgraph, you need a new state for the inner workflow and a way to pop back.

**Fix and rule:** Create `GraphynSubgraphNavigator` that maintains a `var stack by remember { mutableStateOf(emptyList<SubgraphFrame>()) }`. Each `SubgraphFrame(label, state)` holds its own `GraphynEditorState`. The active state is `stack.lastOrNull()?.state ?: rootState`. Pass `dependencies.copy(onEnterSubgraph = { label, inner -> stack = stack + SubgraphFrame(...) })` to the inner shell. The `onEnterSubgraph` lambda reads/writes `stack` through the `MutableState` delegate at call time — capturing the property (not a snapshot) means it always sees the current list.

## `remember(key)` Lambda Can Close Over MutableState Delegates Safely

**Category:** Compose — remember + mutableStateOf interaction

**Problem:** When a `remember { ... }` block creates a lambda that reads/writes a `var foo by remember { mutableStateOf(...) }` property, it's tempting to add `foo` as a remember key to "refresh" the lambda when state changes. But adding a `MutableState`-backed property as a key causes the entire remembered object to be recreated on every state change.

**Fix and rule:** Omit the state from the remember key. The `by` delegate desugars to reading/writing `.value` on the `MutableState` at call time. The lambda always sees the current value without needing to be recreated. Only add a key when the lambda's behavior depends on a stable, non-state value (like a registry or a callback reference).

## GraphynTheme Already Wraps Content in GraphynDsTheme — No Double Setup Needed

**Category:** Design system — CompositionLocal layering

**Problem:** It looks like `GraphynDs.colors` would only be available inside `GraphynDsTheme`. Composables placed above the shell (e.g., a launcher screen, a breadcrumb bar) seem to be outside `GraphynDsTheme` and thus might produce wrong colors or crash.

**Fix and rule:** `GraphynTheme` (the outer, app-level theme) calls `GraphynDsTheme` internally. Anything rendered inside `GraphynTheme` already has access to `GraphynDs.colors` / `GraphynDs.type` with correct branding. The shell's own `GraphynDsTheme` nested inside is redundant but harmless. Do not add a second `GraphynDsTheme` to a composable that will always be called inside `GraphynTheme` — it adds needless complexity. The `LocalGraphynDsColors` also has a non-null default (`GraphynDsColors.Dark`), so accessing it without any theme set returns dark defaults rather than crashing.

## Launcher + Navigator Pattern for Workflow Management

**Category:** Compose — multi-screen flow without NavHost

**Problem:** Need a home screen (templates, recents) that transitions into the workflow editor, with a way to return.

**Fix and rule:** Use a simple `var openWorkflow: WorkflowDefinition?` in the host. When null, show `GraphynWorkflowLauncher`; otherwise show `GraphynSubgraphNavigator` inside `key(wf.id)`. Pass `onHome = { openWorkflow = null }` to the navigator so it shows a "⌂" home button in the nav bar. The navigator shows this bar whenever `onHome != null` or the user is inside a subgraph. Recents are a `mutableStateOf(emptyList<WorkflowTemplate>())` updated on each open, with dedup by workflow ID.

## Dynamically-Sized Cards Must Set Their Own Size via `Modifier.size()`

**Category:** Compose layout — card sizing in unconstrained parents

**Problem:** Sticky note cards placed in `Box(Modifier.offset { pos })` are in an unconstrained container. `fillMaxSize()` inside an unconstrained Box resolves to 0×0, making the card invisible or a single-line label.

**Fix and rule:** Cards that need a fixed or user-controlled size must apply `Modifier.size(w.dp, h.dp)` explicitly. Store user-adjusted dimensions in node config (e.g., `__w` / `__h` keys); read them at render time and fall back to constants. Never rely on `fillMaxSize()` to fill an unconstrained canvas slot.

---

## Custom canvas cards are only draggable where `pointerInput` is applied

**Category:** Compose gestures — custom node cards

**Problem:** Placing `pointerInput` with the drag gesture only on the header `Row` means the card body is inert — dragging from the body does nothing.

**Fix and rule:** Apply the `pointerInput` drag handler to the outermost container (`Column` or `Box`) of a custom card so every pixel is draggable. The header can still have its own `clickable` for selection — that handler fires on tap, while the drag handler activates only once the touch-slop threshold is exceeded, so they do not conflict.

---

## Threading callbacks into canvas cards via `NodeCanvasContext`

**Category:** Architecture — editor extension points

**Problem:** Custom canvas cards (registered via `NodeCanvasFactory`) have no way to trigger shell-level callbacks (like entering a subgraph) because `NodeCanvasContext` previously only contained data and movement/selection callbacks.

**Fix and rule:** Add nullable callback fields to `NodeCanvasContext` for optional host-level actions. Thread them down: `GraphynEditorShellDependencies` → `GraphynCanvasSurface` → `GraphynNodeLayer` → per-node context creation. The context field is null when the host doesn't support the action, letting cards render without a footer button in that case.

---

## Floating breadcrumb overlay vs. layout row

**Category:** UX — subgraph navigation

**Problem:** Placing the breadcrumb as the first child in a `Column` above `GraphynEditorShell` adds it to the layout flow, shrinking the canvas and making it easy to miss — users reported "no way to exit subgraph view".

**Fix and rule:** Switch to a `Box` wrapper so `GraphynEditorShell` fills the entire area, then render the breadcrumb pill as a `Box`-aligned overlay (`Alignment.TopStart` with canvas-relative padding). The pill uses a semi-transparent `panelBackground` and rounded corners so it visually floats. This keeps the canvas full-height and makes the nav affordance more discoverable.

---

## Custom canvas cards must test their own `executionStatus` rendering

**Category:** Testing — custom node card coverage

**Problem:** `SubgraphCard` rendered `NodeStatusBadge` was completely absent — `ctx.executionStatus` was ignored. The bug was invisible because there was no test specifically for custom card execution state, and the visual gap only shows at runtime when you run a workflow.

**Fix and rule:** Custom cards (`NodeCanvasFactory` implementations) must overlay `NodeStatusBadge` (or `GraphynNodeStatusBadge`) and provide a jvmTest that checks badge text (`"+"`, `"v"`, `"x"`) for each `NodeExecutionStatus` value. `app/app` can host its own `jvmTest` source set with `compose.desktop.uiTestJUnit4` — no roborazzi plugin needed for behavior-only tests. Pattern for the overlay: wrap the card in `Box(Modifier.size(...))` and add `NodeStatusBadge(ctx.executionStatus, Modifier.align(Alignment.TopEnd).padding(4.dp), surfaceColor = cardBg)`.


---

## Compose test `captureToImage` captures outer Box, not inner Canvas when padding is applied

**Category:** Testing — Compose UI screenshot tests

**Problem:** `captureToImage()` on a `testTag` applied to a Box that has `padding(4.dp)` returns an image whose coordinates include the padding. If the test computes `calculateMinimapLayout(minimapSize = IntSize(image.width, image.height))`, the layout is based on the OUTER size, but the actual drawing code inside uses an inner Canvas whose `onSizeChanged` reports a SMALLER size (outer minus `2 × padding`). The mismatch shifts the test's computed viewport rect by `paddingPx` pixels, causing sample points to land in the padding background zone instead of on the drawn stroke.

**Fix and rule:** Apply a dedicated `testTag("minimap-canvas")` to the **inner Canvas** composable (where drawing happens), not only to the outer Box. The test then captures the Canvas directly — its image dimensions equal the `minimapSize` tracked by `onSizeChanged`, so `calculateMinimapLayout` in the test receives the exact same input as the composable. No padding offset arithmetic needed.

---

## Auto-layout band-height must be at least the node's own height

**Category:** Canvas layout — auto-layout tree packing

**Problem:** For internal nodes (those with children), `bandH[node]` was set to `sum(children.bandH)`. If children are shorter than the parent (e.g., a FieldCard parent → ShapeCard child: 169dp vs 82dp), `bandH < nodeHeight`, and `y = bandStart + (bandH - nodeHeight) / 2` computes a negative y-offset. This causes the node to render above its own band, overlapping siblings.

**Fix and rule:** Use `maxOf(fallbackH(id), children.sumOf {...})` so a parent node's band is never smaller than `nodeHeight + VERT_GAP`. This guarantees `(bandH - nodeHeight) / 2 ≥ VERT_GAP / 2 > 0` always.

---

## Auto-layout must know actual node sizes and the logical canvas center

**Category:** Canvas layout — auto-layout algorithm

**Problem:** `GraphynAutoLayout.computePositions` used fixed `COL_GAP=320` and `ROW_GAP=220` constants with no awareness of actual node dimensions. `fitToPositions` also used hardcoded `nodeWidth=280, nodeHeight=180`. The layout was placed starting at world `(0,0)` instead of the centre of the 4096×3072 logical canvas, making auto-layout nodes appear far from the visible canvas area.

**Fix and rule:**
- Pass `nodeSize: (nodeType: String) -> IntSize` into `computePositions`. Column x positions accumulate per-column max width + `HORIZ_GAP`. Band heights use `nodeHeight + VERT_GAP` per leaf, summed up the tree.
- After computing positions, shift the entire layout so its bounding-box centre lands on `(DefaultLogicalCanvasWidth/2, DefaultLogicalCanvasHeight/2)` = `(2048, 1536)`.
- `fitToPositions` now takes `Map<String, IntSize>` for per-node bounds; falls back to `GraphynCanvasMetrics.NodeSize` when a node type is unregistered.
- `GraphynEditorState.canvasCards: NodeCanvasRegistry?` is set via `SideEffect` in `GraphynEditorShellContent` so `performAutoLayout` can resolve per-type factory sizes at dispatch time.

---

## `Map.getOrDefault` is not available in Kotlin commonMain

**Category:** KMP stdlib — `commonMain` / multiplatform compatibility

**Problem:** `Map<K,V>.getOrDefault(key, default)` compiles on JVM but not in `commonMain`
because it's a JDK extension, not part of the Kotlin stdlib's common set.

**Fix and rule:** Use `map.getOrElse(key) { default }` everywhere in commonMain. It is
available in all targets and avoids the JVM-specific extension.

---

## `NodeExecutorRegistry` Has No `all()` Method — Use `WorkflowExecutionEngine` Directly

**Category:** Plugin integration — consumer app pattern

**Problem:** Trying to transfer executors from one `NodeExecutorRegistry` to another via `registry.all()` fails to compile because `NodeExecutorRegistry` only exposes `resolve(type)` and `register(type, executor)`. There is no `all()` method (unlike `NodeSpecRegistry` which does have one).

**Fix and rule:** Pass the registry from `DefaultGraphynPluginRegistry` directly to `WorkflowExecutionEngine(plugins.nodeExecutors, plugins.nodeSpecs)` without copying. The plugin registry's registries implement the exact interfaces `WorkflowExecutionEngine` accepts, so no transfer is needed.

---

## LaunchedEffect with stable key doesn't restart on repeated identical input

**Category:** Compose effects — `GraphynCanvasSurface`

**Problem:** `LaunchedEffect(nodeId, portName)` was used to auto-dismiss the type-mismatch
toast after 2 s. If the same port was rejected twice in quick succession (before the first
timer expired), the key didn't change so the effect didn't restart — the second rejection
was silently absorbed and the toast didn't reset.

**Fix and rule:** Whenever an effect must re-run on logically repeated events, the key must
include a monotonic nonce. Added `rejectConnectionPort(nodeId, portName)` to `GraphynEditorState`
which increments a private `_rejectionSerial` counter and stores a `Triple<String, String, Int>`.
The composable uses `LaunchedEffect(rejection)` on the whole triple, so every new rejection
event — even to the same port — gets a distinct key and a fresh timer.

---

## Drag on Outer Box vs Header — When to Use Each

**Category:** Compose gestures — canvas card drag

**Problem:** Moving drag gesture detection to the header (to avoid competition with interactive child widgets like value chips) means the rest of the card body is a dead zone for drag. For cards with a small header and a large body of non-interactive content (port label rows, dividers), the user can't drag from most of the visible card area.

**Fix and rule:** Put the `pointerInput` drag on the **outer Box** of the card, not the header. Interactive children (`BasicTextField`, `clickable` chips, steppers) naturally absorb pointer events within their own bounds — they never propagate up. Non-interactive children (labels, dividers, port rows) do not consume events, so the outer drag fires from those areas. The net result: drag works everywhere except inside interactive children. `FieldCard` keeps drag on the header because its entire body is interactive chips. `ScriptCard` puts drag on the outer box because only the `BasicTextField` area is interactive; port rows above and below are dead space that should be draggable.

---

## JVM-Only Plugin with Custom Canvas Card

**Category:** Plugin architecture — KMP boundary

**Problem:** A JVM-only plugin (`plugins/script`) needs a custom `NodeCanvasFactory` with Compose. The plugin can't use `ui:cards` `internal` helpers, and `app/app` is KMP so it can't host JVM-only UI.

**Fix and rule:** The JVM-only plugin (`kotlinJvm`) depends on `ui:cards` and `editor-api` and defines its own `NodeCanvasFactory` implementation (e.g., `ScriptCardFactory`). The editor plugin registers it via `registrar.registerCanvasCard(type, ScriptCardFactory)`. The KMP `app/app` module uses the node type string `"script.eval"` with no import — it compiles on all targets. The JVM-only plugin is wired in `app/desktopApp/main.kt` which is already JVM-only.

---

## Config-Only Fields vs Port Inputs in NodeSpec

**Category:** Plugin design — spec authoring

**Problem:** A node has a field the user edits inline on the card (e.g., a code editor for a script) but it should not be a connectable wire port. Initially modelled as `PortSpec("code", ...)` in `inputs`, which showed it as a connectable port in the canvas and created an unnecessary wire target.

**Fix and rule:** Fields that are purely user-editable config (not wired from other nodes) belong in `NodeSpec.defaultValues` only — not in `inputs`. The execution engine merges `spec.defaultValues + node.config + connectedPortInputs` before calling the executor (see `WorkflowExecutionEngine.buildInputMap`), so the executor receives the config key just as if it were a port. The card reads it from `ctx.node.config[key] ?: ctx.spec.defaultValues[key]` and calls `ctx.onConfigChange` on edit.

---

## AutoLayout + fitToContent: Dispatch Order and Size Awareness

**Category:** Canvas state — MVI dispatch, viewport fitting

**Problem:** `performAutoLayout()` was calling `fitToPositions` directly with `maxScale=5.0f`, causing over-zoom. Additionally, `fitToContent()` used a hardcoded default size (280×180) for every node, so the bounding box center was miscalculated when actual card sizes differed (e.g. ScriptCard is 320×248).

**Fix and rule:** Remove `fitToPositions` from `performAutoLayout()` — it only sets node positions. The dispatch handler calls `{ performAutoLayout(); fitToContent() }` in sequence. `fitToContent()` resolves actual sizes from `state.canvasCards` and caps scale at 1.0f. Always separate layout (positions) from viewport fitting (scale + offset) in MVI dispatch.

---

---

## Convention Plugin `libs` Accessor Not Available at Compile Time

**Category:** Gradle — included builds, convention plugins

**Problem:** In a precompiled script plugin (`.gradle.kts` file inside `build-logic/src/main/kotlin/`), writing `libs.versions.android.compileSdk.get()` compiles correctly in a regular build script but fails in a convention plugin with "Unresolved reference 'libs'". The type-safe `libs` accessor is generated per-project and isn't on the convention plugin's compile classpath.

**Fix and rule:** Use the `VersionCatalogsExtension` API directly in the convention plugin body:
```kotlin
private val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
compileSdk = catalog.findVersion("android-compileSdk").get().requiredVersion.toInt()
implementation(catalog.findLibrary("kotlin-test").get())
```
The TOML key name (with hyphens) is passed as-is to `findVersion`/`findLibrary`.

---

## `org.jetbrains.kotlin.plugin.compose` Can't Use `id()` in Convention Plugin `plugins {}` Block

**Category:** Gradle — included builds, compose compiler plugin resolution

**Problem:** Writing `id("org.jetbrains.kotlin.plugin.compose")` in a precompiled convention plugin's `plugins {}` block fails during `generatePrecompiledScriptPluginAccessors` with "Plugin was not found in any sources". Even with the plugin's JAR on the build-logic classpath and `pluginManagement.plugins {}` version pins in settings, Gradle can't resolve it for accessor generation.

**Fix and rule:** Apply the compose compiler plugin imperatively with `apply(plugin = "org.jetbrains.kotlin.plugin.compose")` outside the `plugins {}` block. The `plugins {}` block handles `graphyn-kmp-library` and `org.jetbrains.compose`; the compose compiler plugin is applied separately via `apply()`.

---

## `NodeExecutor.execute()` Is Suspend — Tests Need `runTest {}`

**Category:** Testing — plugin unit tests, suspend functions

**Problem:** Plugin tests that call `executor.execute(input)` fail to compile because `NodeExecutor.execute()` is a `suspend fun`. Regular `@Test` functions aren't suspend contexts.

**Fix and rule:** Wrap all calls to `execute()` in `runTest { }` from `kotlinx-coroutines-test`. Add `implementation(libs.kotlinx.coroutinesTest)` to each plugin's `commonTest` dependencies (or to the convention plugin if all modules need it). Non-suspend tests (checking spec counts with `registry.nodeSpecs.all().size`) do not need `runTest`.

---

## Design Token Gap: `AppSpacing` Exists But ui/cards Used Raw `.dp` Literals

**Category:** Design system — token adoption

**Problem:** `ui/cards` depends on `core:designsystem` which exports `AppSpacing` (xxs=2, xs=4, sm=8, …) and `AppShapes` (xs=2, sm=4, md=6, …). All three FieldCard composable files used raw `.dp` literals (8.dp padding, 4.dp gap, 2.dp corner radius, 6.dp corner radius) instead of the tokens, making theme consistency impossible to enforce centrally.

**Fix and rule:** Replace `.dp` literals that match a token with `appTheme.spacing.*` / `appTheme.shapes.*` in `@Composable` functions. Keep structural card dimension constants (`CARD_WIDTH_DP`, `RECORD_POPUP_MIN_DP`, etc.) as `internal const val` in `FieldCardFactory.kt` so they move with the layout math. Keep 1.dp border widths and values that don't map to any token (3.dp, 5.dp, 6.dp spacing) as literals.

---

## Auto-Layout Gaps Must Be Proportional to Node Size

**Category:** Canvas layout — auto-layout spacing

**Problem:** Hardcoded gap constants (`HORIZ_GAP = 200`, `VERT_GAP = 120`) caused node overlap whenever actual card sizes exceeded the assumed baseline. A FieldCard at 240dp wide and a SubgraphCard at 280dp wide require different breathing room — one constant can't serve both.

**Fix and rule:** Remove all constant gaps from `GraphynAutoLayout.computePositions`. After building the `sizes` map, derive gaps from the actual max node dimensions in the current layout set:
```kotlin
val maxW = sizes.values.maxOf { it.width }.coerceAtLeast(GraphynCanvasMetrics.NodeSize.width)
val maxH = sizes.values.maxOf { it.height }.coerceAtLeast(GraphynCanvasMetrics.NodeSize.height)
val horizGap = (maxW * 1.5f).toInt()
val vertGap  = (maxH * 1.5f).toInt()
```
The 1.5× multiplier ensures visible gaps on the canvas **and** in the minimap (see lesson below). The `coerceAtLeast` guards against empty-factory fallback producing zero.

---

## Minimap 2× Node Rendering Inflates Gaps Visually

**Category:** Canvas rendering — minimap accuracy

**Problem:** The minimap draws node markers at `nodeSize * scale * 2f` for visual weight (so small nodes don't vanish at minimap scale). The side effect: a gap equal to 1× node size on the canvas looks like zero gap in the minimap, because each node dot already occupies 2× its proportional world area. Tests can assert no overlap (gap ≥ 0) and still produce a minimap where nodes appear to touch.

**Rule:**
- A gap of **< 1× node size** → nodes overlap in the minimap.
- A gap of **1× node size** → nodes exactly touch in the minimap (still looks cramped).
- A gap of **1.5× node size** → clear breathing room in both canvas and minimap.

The 2× minimap multiplier is intentional (for readability at small sizes). Design auto-layout gaps with this in mind: the canvas gap needs to be at least 1× the node dimension for the minimap to show any separation at all.

---

## JVM-Only Plugin Modules Need `src/test/kotlin`, Not `src/commonTest`

**Category:** Testing — module structure

**Problem:** The `script` plugin uses `alias(libs.plugins.kotlinJvm)` (not KMP), so its source layout is `src/main/kotlin` / `src/test/kotlin`. Adding a `src/commonTest` directory does nothing for JVM-only modules. Use `testImplementation(libs.kotlin.test)` in the `dependencies {}` block (not `commonTest.dependencies {}` in a `kotlin {}` block).
---

## "Auto-Layout Not Centered" Was the MinScale Floor Clamping Fit-to-Content

**Category:** Viewport — fit-to-content

**Problem:** In a narrow window, after Auto Layout the rightmost node was clipped under the inspector (and the leftmost under the palette) — symmetric overflow. The centering math was fine; the bug was the scale floor. `fitToPositions` computed `scale = minOf(fitX, fitY, maxScale).coerceAtLeast(MinScale)`, where `MinScale = 0.45f` is the *interactive* zoom-out limit. A 1640-wide layout in a 640px canvas needs `scale ≈ 0.317` to fit, but the `coerceAtLeast(MinScale)` floored it to 0.45, so the content was too big and spilled past **both** edges (live log: `scale=0.45 Lgap=-49 Rgap=-49`). In a *wide* window the needed scale was above 0.45, so it fit — which is why the bug was intermittent and depended on window width.

**Why it took so long to find:** Roborazzi captures run at a fixed wide headless size (canvas 1440) where the required scale stayed above MinScale, so the bug never reproduced in tests or screenshots. The headless capture diverged from the live narrow window. Ground truth came from logging `canvasSize` + fit results to the app's own LOGS panel and having the user read them off the **live** app — not from any rendered screenshot.

**Rule:**
- Fit-to-content needs its **own** minimum-scale floor (`MinFitScale`, here 0.05f), separate from the interactive `MinScale`. Flooring a fit at the interactive zoom limit clips wide content in small viewports.
- When a rendered test capture and the live app disagree, trust the live app. Surface real numbers from the running app (here via `log.push` → LOGS panel) instead of pixel-measuring screenshots — headless capture sizes can hide size-dependent bugs.
- `FitToContentTest.wideContentIsContainedInNarrowCanvas` locks this in: 1640-wide content in a 640px canvas must have both edges inside the canvas.

---

## Auto-dismissing animations make screenshot tests flaky

**Category:** Compose UI testing — animation clocks
**Applies to:** Any composable that fades/animates itself away on a timer, captured via `captureToImage()`

### Pattern

`GraphynMinimapDebugger` uses `LaunchedEffect(state.viewport) { alpha.animateTo(0.9f); delay(1500); alpha.animateTo(0f) }`
to flash the minimap and fade it out. Under the default test clock, `waitForIdle()` / autoAdvance
runs the clock to quiescence — past the 1.5s hold — so by capture time the minimap is fully
transparent and every pixel equals the background. The assertion "border != outside" then fails
even though production behaviour is correct.

**Rule:**
- Don't let a self-dismissing animation reach its end state before you capture. Disable
  `rule.mainClock.autoAdvance`, then `advanceTimeByFrame()` + `advanceTimeBy(n)` to land on the
  *visible* plateau (after fade-in, before the hold expires). Capture there.
- Keep the fix in the test only — the live fade behaviour is intentional and correct.

---

## Truncating a sub-pixel rect edge can sample the wrong pixel

**Category:** Compose UI testing — pixel sampling
**Applies to:** Reading specific pixels from `captureToImage().toPixelMap()` against a drawn shape

### Pattern

The minimap viewport rect's left edge sat at `x=14.7`. A 2px stroke centred on that edge paints
solid colour at pixels 15–17, but `viewportRect.left.toInt()` truncates to `14` — a background
pixel just outside the stroke — so `border == outside`. The test had passed only because earlier
geometry happened to put the edge on an integer; commit 363e09d shifted it sub-pixel and exposed
the latent bug.

**Rule:**
- Use `roundToInt()`, not `toInt()`, when picking the pixel that should land *on* a drawn edge —
  truncation biases toward the lower neighbour and can miss a thin stroke entirely.

---

## Resilient execution changes the "missing executor" contract

**Category:** Workflow execution — engine semantics

**Problem:** Executor v2 wraps each node in try/catch so one failing node no longer aborts the whole run — it's recorded as `NodeExecutionStatus.Error`, its transitive dependents become `Skipped`, and independent branches still execute. This silently changes a previously-throwing path: a node with no registered executor used to throw `WorkflowExecutionException` out of `execute()`; now it's a per-node error in the result. Any test or caller asserting "the whole run throws" must instead inspect `result.statusByNodeId` / `errorsByNodeId`.

**Rule:**
- Distinguish **structural** failures (duplicate ids, cycle) — which still throw before any node runs — from **per-node** failures, which are captured in the result. Only the former abort.
- When making execution resilient, grep for `assertFailsWith` / bulk error-marking in callers; the editor's old `execute()` marked *every* node Error on any exception and must switch to the engine's per-node status.

---

## NullableType inputs are still `required` unless you say otherwise

**Category:** Plugin authoring — NodeSpec ports

**Problem:** `PortSpec.required` defaults to `true`, independently of the port's `WorkflowType`. `io.http_request` declared `body`/`headers` as `NullableType(...)` — semantically optional — but left `required` at its default, so `WorkflowGraphValidator` flagged `missing_required_input` on any http_request node that didn't wire those ports or supply a config/default. The node looked optional but wasn't.

**Rule:**
- A `NullableType` port is not automatically optional. Set `required = false` for ports that are genuinely optional, or give them a `defaultValues` entry. Nullability describes the value; `required` describes whether the port must be satisfied.

---

## @Serializable needs the compiler plugin in the *defining* module

**Category:** kotlinx-serialization — multi-module setup

**Problem:** The server (`:server`, a plain `kotlinJvm` module) defined a `@Serializable`
DTO and got a runtime `SerializationException: Serializer for class 'RunAccepted' is not
found` — even though serializing `:core` types from the same module worked fine. The
serialization *compiler plugin* only generates serializers for `@Serializable` classes in
modules that **apply the plugin**. `:core` applies it, so its types carry generated
serializers across the dependency edge; `:server` did not, so its own annotated classes had
none.

**Rule:** Every module that *declares* its own `@Serializable` types must apply
`alias(libs.plugins.serialization)` (or the KMP equivalent). Depending on a module that has
the plugin is not enough — it only covers that module's own classes.

---

## SSE frame data must be single-line — don't pretty-print it

**Category:** Ktor — server-sent events

**Problem:** Streaming `ServerSentEvent(data = prettyJson.encodeToString(...))` produced
frames the client couldn't parse (`Unexpected JSON token ... JSON input: {`). SSE delimits
frames by blank lines and treats each `\n` inside `data` as a separate `data:` line, so a
pretty-printed (multi-line) JSON object is split across several `data:` lines and no longer
decodes as one value.

**Rule:** Encode SSE frame payloads with a **compact** `Json` (no `prettyPrint`). Pretty
output is fine for ordinary response bodies but never for `text/event-stream` data.

---

## `@Serializable` on existing model types enables direct store snapshots

**Category:** KMP — serialization, persistence

**Problem:** `NodeRef`, `ConnectionRef`, and `WorkflowDefinition` were not `@Serializable`.
The `WorkflowDocumentCodec` worked around this by mapping to DTO types, but it silently
dropped `NodeRef.subgraph` (recursive field). Any store that needed to persist full
snapshots either had to go through the codec (losing subgraphs) or duplicate the graph model.

**Rule:** Annotate the core graph model types with `@Serializable` directly. The serializer
handles recursive types (`WorkflowDefinition` → `NodeRef.subgraph: WorkflowDefinition?`)
correctly. The existing `WorkflowDocumentCodec` remains valid for the versioned document
format; `@Serializable` is additive and does not replace it. Always annotate foundational
data-model classes at the time they're defined — retrofitting later forces consumers to
update all serialization paths simultaneously.

---

## `kotlinx-datetime` is the correct KMP clock — avoid `expect/actual` for time

**Category:** KMP — cross-platform utilities

**Problem:** Needing `currentTimeMillis()` in `commonMain` requires platform-specific
implementations for JVM, JS, wasmJs, Android, iosArm64, iosSimulatorArm64 — six files for
one function when using `expect/actual`.

**Rule:** Add `org.jetbrains.kotlinx:kotlinx-datetime` to the catalog and use
`Clock.System.now().toEpochMilliseconds()` in `commonMain`. The library ships a single
multiplatform artifact that covers every Graphyn target without extra source sets. Reserve
`expect/actual` for behavior that genuinely differs by platform, not for stdlib gaps that
a JetBrains library already bridges.

---

## `kotlinx-datetime` is `implementation`, not `api` — not transitively available to consumers

**Category:** KMP — module dependency graph

**Problem:** `core/build.gradle.kts` declares `implementation(libs.kotlinx.datetime)`.
Modules that depend on `:core` via `api(projects.core)` (e.g., `app/shared`, `app/app`)
do not get `Clock.System` transitively — `implementation` deps are not exported.
Attempting to use `Clock.System.now()` in `app/app/commonMain` fails to compile.

**Rule:** For IDs in `commonMain` without adding a new dep, use `kotlin.random.Random.nextLong()` from the stdlib. Only add `kotlinx-datetime` to a consuming module if it needs clock access beyond what comes through `:core`'s public API.

---

## `LaunchedEffect` cannot be called inside a non-composable lambda

**Category:** Compose — correctness

**Problem:** Trying to call `LaunchedEffect(id) { ... }` inside an `onClick` lambda (non-composable scope) is a compile error; composables can only be called from `@Composable` functions.

**Rule:** Use a state variable as the trigger (`var pendingLoadId by remember { ... }`), set it in the lambda, and observe it with a top-level `LaunchedEffect(pendingLoadId)` in the composable body.

---

## `kotlinx.browser` is JS-only — wasmJs needs `@JsFun` interop

**Category:** KMP — wasmJs platform target

**Problem:** `import kotlinx.browser.window` compiles fine in `jsMain` but fails with "Unresolved reference 'browser'" in `wasmJsMain`. The `kotlinx.browser` package is a Kotlin/JS-only artifact and is not available to the Wasm target.

**Rule:** In `wasmJsMain`, access `localStorage` (and other browser globals) via `@JsFun` external declarations:
```kotlin
@JsFun("(key) => localStorage.getItem(key)")
private external fun lsGetItem(key: String): String?
```
Also avoid `Json.decodeFromString<T>()` with inferred type params in wasmJs — use explicit serializer: `decodeFromString(MyType.serializer(), raw)`.

---

## `web.window.window` does not expose `matchMedia` — use `kotlinx.browser.window`

**Category:** KMP — jsMain browser APIs

**Problem:** `import web.window.window` (from the `web` interop package) gives a `Window` type that lacks `matchMedia`. Calling `window.matchMedia(...)` fails with "Unresolved reference".

**Rule:** For media queries in `jsMain`, use `import kotlinx.browser.window` which exposes the full browser Window API including `matchMedia`.

---

## `channelFlow` bridges a callback API into a `Flow`

**Category:** Kotlin coroutines — Flow

**Problem:** `WorkflowExecutionEngine.execute()` takes an `onEvent: (ExecutionEvent) -> Unit` callback but callers (CLI, tests, custom UIs) prefer a `Flow`. Converting with `callbackFlow` is safe only when the callback is synchronous (which this one is — it runs on the engine's calling coroutine).

**Rule:** When the callback is synchronous (called from the same coroutine scope, not dispatched elsewhere), use `channelFlow` + `trySend`:
```kotlin
channelFlow {
    val result = execute(workflow) { event -> trySend(ExecutionStreamMessage.Event(event)) }
    send(ExecutionStreamMessage.Completed(result))
}
```
`trySend` never throws; `send` (for the terminal frame) uses suspension. Don't use `callbackFlow` for async callbacks that outlive the collector's scope.

---

## Adding a trailing defaulted parameter breaks trailing-lambda call sites

**Category:** Kotlin — API evolution

**Problem:** `WorkflowExecutionEngine.execute(workflow, onEvent)` was called as `execute(w) { event -> ... }` (trailing lambda binds to `onEvent`). Adding a third defaulted param `externalInputs: Map<...> = emptyMap()` after `onEvent` silently broke every trailing-lambda call site — the lambda no longer binds to `onEvent` because it isn't the last parameter, producing a confusing "argument type mismatch" at the lambda.

**Rule:** When a function may be called with a trailing lambda, keep the function-type parameter **last**, or add new params *before* it. If that's not possible, convert call sites to the named form `execute(w, onEvent = { ... })`. Grep for every `.execute(` call after changing such a signature — the compiler error points at the lambda, not the real cause.

---

## JVM plugin auto-discovery via `ServiceLoader` in a KMP `expect/actual`

**Category:** KMP — platform capabilities

**Problem:** `java.util.ServiceLoader` is JVM/Android-only, but the plugin contract lives in multiplatform `commonMain`. Hosts wanted classpath auto-discovery without a JVM-only API leaking into common code.

**Rule:** Expose `expect fun discoverGraphynPlugins(): List<GraphynPlugin>` in `commonMain`; the JVM and Android actuals use `ServiceLoader.load(GraphynPlugin::class.java)`, and JS/Wasm/iOS actuals return `emptyList()`. Test fixtures register via `src/jvmTest/resources/META-INF/services/<fqcn>` and need a public no-arg constructor (or be a Kotlin `object`).

---

## A subgraph node's ports are derivable from its inner workflow — don't store them

**Category:** Architecture — avoiding redundant state

**Problem:** Collapsing a selection into a subgraph node needs the node to expose boundary ports, but the registry maps one `NodeSpec` per *type*, so a collapsed node can't carry per-instance ports that way. Storing a synthetic spec on the node (or in a parallel map) risks staleness when the inner workflow changes.

**Rule:** Derive the spec on demand from the inner workflow's boundary — *free input ports* (no internal connection targets them) become inputs, *free output ports* (no internal connection consumes them) become outputs (`deriveSubgraphSpec`). The canvas/inspector resolve `registry.resolve(type) ?: deriveSubgraphSpec(node, registry)`, so a registered spec always wins (demo subgraph nodes unaffected) and collapsed nodes need no stored spec. This same "free port" boundary is what the engine uses for input injection and free-output collection — one consistent rule end to end.

---

## Subgraph outputs should be *all* free outputs, not the last-executed node

**Category:** Execution engine — correctness

**Problem:** The engine exposed a subgraph node's outputs as `executionOrder.lastOrNull()`'s outputs. That works for a linear chain but is wrong for a collapsed selection whose boundary output comes from a node that isn't executed last.

**Rule:** A subgraph's outputs are the union of every inner node's output values whose `(node, port)` is not consumed by an internal connection (`freeOutputs`), keyed by port name. This matches the derived-spec boundary and stays correct for arbitrary collapsed graphs. (Verified existing linear-chain subgraph tests still pass.)

---

## Keyboard shortcuts are data + state, not logic branches

**Category:** Architecture — state-driven configuration

**Problem:** Adding configurable shortcuts requires mapping user rebinds to action dispatch, persisting them, detecting conflicts. A naive approach hardcodes checks (if `e.key == Key.Z && e.isPrimaryMeta` → Undo) in gesture handlers — but then rebinding requires rewriting those branches, and conflict detection is diffuse.

**Rule:** Separate *data* (key mappings, defaults) from *behavior* (dispatch logic). Store shortcut bindings in `GraphynShortcutState` (mirrors `GraphynAppearanceState` pattern), which exposes `resolveAction(event): EditorShortcutAction?` and rebind/reset methods. The state owns persistence (overrides to `GraphynSettingsStore` as JSON), not individual branches. Gesture handlers become a simple `when(state.resolveAction(event))` dispatch. This makes rebinding + conflict detection testable in isolation, and shipping changes to defaults (new shortcuts, reordered priorities) doesn't require hunting through gesture code.

**Implementation notes:**
- `EditorShortcutAction` enum: 9 bindable actions with stable id, label, defaultChord.
- `KeyChord` (serializable): keyName + primaryMeta + shift; matches KeyEvent, displays human-readable, conflict-detects.
- `ShortcutKeyTable`: logical key name mapping (A-Z, 0-9, F1-F12) for stable persistence across reboots.
- `GraphynShortcutState`: holds defaults + JSON-persisted overrides, exposes `chordFor(action)`, `resolveAction(event)`, `rebind/resetToDefault/resetAll()`.
- UI: read-only `GraphynShortcutsPanel` (v1); rebind UI + record-next-key flow is next.


---

## Ollama `/api/generate` may stream NDJSON even with `stream=false`

**Category:** LLM integration — HTTP response parsing

**Problem:** `OllamaWorkflowGenerator` sent `stream=false` and decoded the body as a single
`{"response": "..."}` object. Against a reverse-proxied host (`https://…/ollama/`) the body came
back as **NDJSON** — one `{"model":…,"response":"…","done":false}` frame per line — so
`Json.decodeFromString<GenerateResponse>(body)` threw "Expected EOF after parsing, but had {".
Direct `curl` with `stream:false` returned a single object, so the bug only appeared through the
real client path, not the curl smoke test.

**Rule:** Parse Ollama responses defensively as NDJSON regardless of the `stream` flag —
split by lines, decode each, concatenate every frame's `response` field:
```kotlin
body.lineSequence().filter { it.isNotBlank() }
    .mapNotNull { runCatching { json.decodeFromString<GenerateResponse>(it).response }.getOrNull() }
    .joinToString("")
```
This handles both single-object and streamed responses. Verify LLM HTTP integrations through the
actual client code (a `@Ignore`'d live test you run on demand), not just curl — proxies and curl
can disagree on framing.

## LLMs draft graph topology before they fill inputs

When generating workflows, the first thing that worked was *structure* — the model picked correct
node types and wired plausible connections, but left node `config` empty, so every generated node
needed manual input entry. Two changes fixed it: (1) the prompt's node catalog must expose **port
types**, not just names (`http_request — [url:string, method:string] -> [body:string]`), and the
schema must include a per-node `config` object with an explicit instruction to fill every
*unconnected* input with a type-matched literal; (2) the parser must **coerce** each JSON value to
the port's declared `WorkflowType` (JSON numbers/bools/strings → `IntValue`/`BooleanValue`/…,
tolerating stringified numbers), and drop config keys that don't match a real input port. Telling
the model the types is what makes it produce usable literals.

## Surface what the sanitizer dropped, or the user thinks the model failed

`WorkflowJsonParser` defensively drops unknown node types and bad-port connections so generation
never hard-fails. But silently dropping them means a user who asked for a node the catalog doesn't
have just sees a smaller graph with no explanation. The fix is to thread the parser's
`droppedNodes` / `droppedConnections` all the way to the UI as a per-turn **warning** line
("⚠ Skipped unsupported node: foo (mystery) · Dropped 2 invalid connections"). Defensive parsing
and user-visible feedback are two halves of the same feature — don't ship one without the other.

## Kotlin 2.4.0 WasmJS IR deserialization bug

**Category:** Kotlin compiler — IR backend  
**Applies to:** WasmJS/JS targets on Kotlin 2.4.0+

### Issue

Kotlin 2.4.0's WasmJS compiler fails with an internal IR deserialization error when compiling code
that imports complex types (especially those with type aliases or generics). The error occurs during
IR module loading: `IrDeclarationDeserializer.deserializeIrTypeAlias()` fails to deserialize a
symbol table entry, causing compilation to abort with "Internal compiler error."

**Timeline:**
- Kotlin 2.3.x: WasmJS compiles successfully
- Kotlin 2.4.0: WasmJS fails on code with AI assistant module + new types

**Workaround:** Disable WasmJS builds in CI (docs workflow). The app works fine for JVM/native
targets. WasmJS is a web demo only, not critical path.

**Fix:** Downgrade to Kotlin 2.3.x requires downgrading Compose Multiplatform (which dropped 2.3
support), creating a toolchain lock. Wait for Kotlin 2.5.x which is expected to fix IR issues.

**Bug report filed:** [Link TBD] Kotlin issue tracking IR deserialization failure on WasmJS target

---

## A `@Composable` param's ABI is `FunctionN+2` — a consumer module without the Compose compiler plugin sees the wrong arity

**Category:** Compose compiler — cross-module ABI, Gradle plugin setup
**Applies to:** Any module that *constructs* or *calls* a type from another module whose public API includes a `@Composable` function-type parameter (e.g. `ui:cards`'s `ShapeCardFactory(avatar: (@Composable (NodeRef, NodeSpec) -> Unit)?)`)

### Symptom

Runtime `NoSuchMethodError` on a constructor/method, where the **only** difference between the requested and available signature is the function-type arity:
```
NoSuchMethodError: 'void ShapeCardFactory.<init>(Shape, float, ShapeNodeTheme, NodeShape,
    kotlin.jvm.functions.Function2,  ← consumer thinks Function2
    int, FieldNodeTheme, int, DefaultConstructorMarker)'
  at plugins.gmail.GmailEditorPlugin.register(GmailEditorPlugin.kt:22)
```
…but the producer class actually declares `kotlin.jvm.functions.Function4` for that param.

### Root cause (NOT stale builds)

The Compose compiler plugin rewrites every `@Composable` function type by appending two params: `Composer` and a `changed: Int`. So `@Composable (A, B) -> Unit` becomes `Function4`, not `Function2`. A module that depends on `ui:cards` but **does not apply the Compose compiler plugin** does not perform this rewrite when *reading* the dependency's metadata — it sees the raw `(A, B) -> Unit` as `Function2` and emits a call to a constructor that doesn't exist. The producer (`ui:cards`, which has the plugin) emits `Function4`. Mismatch → `NoSuchMethodError` at first use.

This survives `clean`, `--no-build-cache`, and `--rerun-tasks` because it is a *correct* compilation of a module with the *wrong plugin set* — not staleness. The decisive diagnostic is `javap -c` on the consumer's `.class`: if the emitted `<init>` reference uses `FunctionN` while the producer uses `FunctionN+2`, the consumer is missing the Compose compiler plugin.

### Fix

Apply both Compose Gradle plugins to the consumer module (`plugins/gmail`, `plugins/linkedin`):
```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)  // org.jetbrains.compose
    alias(libs.plugins.composeCompiler)       // org.jetbrains.kotlin.plugin.compose — the one that fixes the ABI
    alias(libs.plugins.mavenPublish)
}
```
(`composeCompiler` is the load-bearing one; `composeMultiplatform` keeps the setup consistent with `sample-style-nodes`, which uses the `graphyn-kmp-compose-library` convention plugin that bundles both.)

### Rule

Any module that touches a `@Composable`-typed API surface from another module **must** apply the Compose compiler plugin, even if it only *constructs* the type and never writes a composable itself. Prefer the `graphyn-kmp-compose-library` convention plugin for new Compose-consuming modules so this can't be forgotten. When you see a `NoSuchMethodError` whose only delta is `FunctionN` vs `FunctionN+2`, suspect a missing Compose compiler plugin — not a stale build.

---

## A canvas card's layout width must equal the width its port anchors assume

**Category:** Canvas rendering — node/port alignment
**Applies to:** Any `NodeCanvasFactory` whose `nodeWidth` / `portAnchorY` are computed from a fixed shape size while the card content can be wider

### Symptom

Gmail/LinkedIn circle nodes had their connection dots floating to the left of the circle. `GraphynNodeLayer` draws input ports at `position.x` and output ports at `position.x + factory.nodeWidth`, and `ShapeCardFactory.nodeWidth` is the *shape* size (48dp). But `ShapeCard` laid the shape + label in a `CenterHorizontally` `Column` with **no width constraint**, so a label wider than the shape (e.g. "Fetch Emails") grew the column to the label width and re-centered the circle inside it. The circle ended up around `[22, 70]` while the ports stayed anchored to `[0, 48]`.

### Root cause

The card is placed in `Box(Modifier.offset { position })` — unconstrained width. A centered `Column` then takes the width of its widest child (the label), shifting every narrower child (the shape) right by `(labelWidth - shapeWidth) / 2`. The port-anchor math (`nodeWidth`, `portAnchorY`) had no idea the visual shape had moved.

### Fix

Constrain the card's layout width to the shape size and let the label *overflow* without affecting layout:
```kotlin
val cardWidth = if (hasWidgets) max(size.value.toInt(), CARD_WIDTH_DP).dp else size
Box(Modifier.width(cardWidth)) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(size)...)            // shape now fixed at [0, size]
        BasicText(
            label,
            modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally, unbounded = true),
        )
    }
}
```
`wrapContentWidth(..., unbounded = true)` lets the text measure at its natural width and paint wider than the `size`-wide column while staying centered on the shape — so it never widens the card or moves the shape.

### Rule

Whenever a factory reports a fixed `nodeWidth`/`portAnchorY`, the card composable must actually occupy that width. Any content that can exceed it (labels, badges) must overflow via `wrapContentWidth(unbounded = true)` or an overlay, never by growing the layout. Verify with a Roborazzi capture that overlays port-anchor markers at `x=0` and `x=nodeWidth` (`ShapeNodeAlignmentTest`) — the shape edges must line up with the markers across varying label widths.

## Canvas geometry must resolve factories the same way the node layer does

When `GraphynNodeCard` was retired in favor of a default `FieldCardFactory`, nodes without a
registered card (e.g. `io.resolve_path`) started rendering through the spec-sized default factory.
But `GraphynConnectionLayer`, `GraphynConnectionMidpoints`, and `GraphynMinimapDebugger` still
resolved the factory with `canvasCards.resolve(type)` directly — which returns null for those
nodes and falls back to `GraphynCanvasMetrics` geometry (width 280 vs the card's 240, different
port anchors). The result was connection lines that started ~40dp off the output dot and at the
wrong height ("broken wires"). **Rule:** any code computing node geometry (width, port anchors)
must go through `resolveNodeFactory(node, canvasCards, nodeSpecs)`, never `canvasCards.resolve`
directly, so it agrees with what the node layer actually rendered.

## Annotation nodes need a no-op executor to live in an executable workflow

`isAnnotation` is an editor-api concept (on the card factory); the core `WorkflowExecutionEngine`
doesn't know about it and **throws** `"No executor registered for node type '…'"` for any node
lacking an executor. So embedding a `graphyn.sticky_note` guide directly in a template's
`WorkflowDefinition` would fail execution. Fix: `StickyNotePlugin` registers a no-op executor
(`{ emptyMap() }`) so the annotation is a harmless pass-through. Same applies to any annotation you
want to ship inside an executable template.

## FieldCard's clickable merges child semantics — UI tests need useUnmergedTree

The `FieldCard` root has `.clickable { onSelect() }`, which creates a merging semantics boundary.
A `testTag` placed on an inner element (e.g. the header drag handle, `node-header-<id>`) is not
addressable from the merged tree — `onNodeWithTag(tag)` fails with "could not find … However, the
unmerged tree contains 1 node that matches." Use `onNodeWithTag(tag, useUnmergedTree = true)`.
Per-node header tags are also how you disambiguate two nodes that share a label (the default card
shows the spec label, not the node id, so text matching is ambiguous).

## $$ escaping for Kotlin Script embedded in workflow definitions

`script.eval` code written in a triple-quoted Kotlin string inside a `WorkflowDefinition` must
escape its own template variables as `$$name`, otherwise the host compiler interpolates them at
build time. JVM-only APIs (e.g. `String.format`) are fine inside the script — it runs on the JVM,
not in commonMain. See `ScriptSpec` KDoc.

## FFmpeg builds vary wildly — probe for filters, don't assume them

The Phase 2 `media.caption_overlay` node burns subtitles with the `ass` filter, which is only
present when FFmpeg is compiled with libass. Many minimal builds (including some Homebrew/CI ones)
ship without libass — and also without `subtitles` and `drawtext` — so the filter fails at parse
time with a confusing `No such filter: 'ass'` / `Error parsing filterchain`. The fix is twofold:
the backend exposes `supportsFilter(name)` (parses `ffmpeg -hide_banner -filters`) and
`renderCaptionOverlay` checks it up front to throw a clear precondition error; the
availability-guarded backend test skips the caption leg when `ass` is absent (the `overlay`,
`format`, and `colorchannelmixer` filters used by `video_compose` are part of core FFmpeg and are
safe to assume). Rule: any node that depends on an optional FFmpeg filter must probe for it rather
than assume it, and its test must degrade to a no-op when the filter is missing.

## A node that consumes a list-of-records needs a builder + collector to be wireable

`media.video_compose` (`overlays: ListType(videoOverlay)`) and `media.timing_controller`
(`sync_points: ListType(syncPoint)`) were implemented and unit-tested, but **could not be placed in a
demo template** — nothing in the graph produces a `RecordType` value, let alone a list of them. The
type system has no literal/record-constructor, so a `ListType(RecordType(...))` input is a dead port
unless a producer node exists. The fix is the same builder + collector idiom already used for media
handles (`audios_list`/`videos_list`): a **builder** node whose inputs are the record's fields and
whose single output is the record (`media.video_overlay`, `media.sync_point`), plus a **collector**
node that gathers `itemN` inputs into a `ListValue` (`media.overlays_list`, `media.sync_points_list`,
backed by a generic `recordListExecutor(prefix, outputName, label)`). Rule: any node that consumes a
`ListType(RecordType)` you expect users to build by hand needs a matching builder+collector pair, or
it is only reachable from a script.

## Maven Central publishing: three silent failures that produced zero artifacts

From v0.3.0 through v0.6.0, every CI publish job reported success yet **nothing ever reached
`repo1.maven.org`**. Three independent bugs compounded, each silent:

1. **`automaticRelease` defaults to `false`.** The vanniktech `publishToMavenCentral()` call uploads
   a bundle to the Sonatype Central Portal but leaves it in `PENDING`/validated-but-unreleased state
   unless `automaticRelease = true` is passed. The build goes green; the artifact sits in the portal
   forever. Fix: `publishToMavenCentral(automaticRelease = true)` on every module.

2. **Signing was gated on a property CI never sets.** The condition was
   `if (project.hasProperty("signingKey")) signAllPublications()`, but CI exports
   `ORG_GRADLE_PROJECT_signingInMemoryKey` (→ Gradle property `signingInMemoryKey`). `signingKey` is
   never set, so `signAllPublications()` was skipped and no `.asc` files were generated — the portal
   rejects unsigned bundles. Fix: check `signingInMemoryKey`. Rule: the signing guard must name the
   exact property your CI exports; a typo'd guard fails open (skips signing) instead of erroring.

3. **The signing key was not on a public keyserver.** Even with valid `.asc` files, the portal
   rejects them with "Could not find a public key by the key fingerprint" until the *public* half is
   discoverable. Upload it: `keys.openpgp.org` (HTTP API `POST /vks/v1/upload` with JSON
   `{"keytext": "..."}` — note it requires an email-verification click for the UID to appear in
   *searches*, but the key is immediately retrievable *by fingerprint*, which is what the portal
   uses) and `keyserver.ubuntu.com` (`POST /pks/add`, no verification). `gpg --send-keys` failed from
   this network ("Server indicated a failure") — use the HTTP APIs.

### Other publishing gotchas hit in the same session

- **vanniktech 0.37.0 removed `SonatypeHost`.** Central Portal is now the implicit default;
  `publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, ...)` no longer compiles. Drop the arg and the
  import. 0.37.0 also requires Dokka v2: add
  `org.jetbrains.dokka.experimental.gradle.pluginMode=V2EnabledWithHelpers` to `gradle.properties`.
- **"Component is currently being published in another deployment".** Re-running a publish while a
  prior bundle for the same coordinates is still `PUBLISHING` fails validation. Check the portal
  (`GET /api/v1/publisher/deployments`) before retrying — the earlier run may already be succeeding.
  Drop stale `FAILED` deployments with `DELETE /api/v1/publisher/deployment/{id}`.
- **`expect`/`actual` with a missing actual only surfaces at publish time.** `jvmTest` is green
  because the JVM `actual` exists; publishing compiles *all* KMP targets and only then fails
  ("Expected ... has no actual declaration ... for Native"). A canvas-card `expect fun` with a
  JVM-only actual blocked `graphyn-runtime`/`graphyn-editor` for iOS/JS/WASM/Android. Rule: an
  `expect` needs an actual for every target the module (and every aggregator that depends on it)
  publishes — verify with `compileKotlin{IosArm64,Js,WasmJs}` + `compileAndroidMain`, not just tests.
- **Shell: a `declare -a GROUPS=(...)` array was clobbered by a `GROUPS` var sourced from `.env`.**
  `set -a; source .env` ran before the array declaration; the env value won and the loop iterated a
  stray scalar. Rule: name script-local arrays distinctly (e.g. `PUBLISH_GROUPS`) so they can't
  collide with sourced environment variables.

## JNI bridge for large C structs: use a flat C wrapper struct, not 60+ function params

**Category:** JNI / C interop

When a native library exposes a struct with 30+ fields (`sd_ctx_params_t`, `sd_img_gen_params_t`),
do NOT add all fields as individual JNI function parameters — JDK has a practical limit and the
function signature becomes unmaintainable. The correct pattern:
1. Define a **flat C struct** in your `*-wrapper.h` that mirrors the library struct but uses only
   C-primitive types (`const char*`, `int`, `float`, `bool`) — no C++ templates or enum fields
   directly (store enum values as `int`, cast to the enum in the wrapper impl).
2. Add `void wrapper_struct_init(MyStruct* p)` that sets every field to its safe default.
3. JNI bridge builds the flat struct from JVM types (jstring → std::string → c_str(), jboolean →
   bool, etc.), then calls one wrapper function. This IS still "type conversion only" — the JNI
   layer owns no native logic, it just groups the conversions.
4. Kotlin callers use data classes (one per logical group) with sensible defaults; the wrapper
   method unpacks them before calling the JNI external fun.
- **Why:** direct 60-param JNI functions are error-prone to call, easy to get param order wrong,
  and cannot be extended without changing the signature. The struct approach lets you add new fields
  without touching any JNI signature — only the C and Kotlin structs change.

## Wrapper struct default-init is mandatory — zero-init alone is not safe for enum fields

When adding `sd_wrapper_ctx_params_init()` / `sd_wrapper_generate_params_init()`, do NOT rely on
`*p = {}` (zero-init) alone. Library enums may have a 0 value that is NOT the safe default:
- `sd_vae_format_t`: value 0 = `SD_VAE_FORMAT_FLUX` but the safe default is -1 = `SD_VAE_FORMAT_AUTO`.
- `prediction_t`: value 0 = `EPS_PRED` but for FLUX models you want the library to auto-detect.
Rule: always follow `*p = {}` with explicit assignments for every enum/flag that has a "no-op"
sentinel that differs from zero (e.g. -1 = auto, or a specific enum constant).

---

## C header forward-declaration order: new types must come AFTER existing type aliases

**Category:** C/C++ — header authoring

When adding new structs to a shared `*-wrapper.h` that already defines types (typedef'd result
structs, callback typedefs, opaque handle types), placing the new structs BEFORE the existing
`typedef` blocks causes `unknown type name` errors because the new types reference the older ones.

**Rule:** In a C header, always place new structs AFTER all existing type definitions they reference.
The compiler reads a header top-to-bottom; a forward reference to a not-yet-seen typedef is an error.
When in doubt, put all new additions at the **bottom** of the header's type section.

---

## `*p = {}` on a `typedef struct` is a C++ feature — use `memset` for C-compatible init functions

**Category:** C/C++ — struct initialization in shared headers

In `*-wrapper.h` files that use C linkage (`extern "C"`) or will be compiled by a C compiler,
`static inline void init(MyStruct* p) { *p = {}; }` causes "no viable overloaded '='" errors.
`{}` zero-initialization on a C-style `typedef struct` is C++11, not C99.

**Rule:** Use `memset(p, 0, sizeof(*p))` in `static inline` init functions defined in shared headers.
Follow with explicit field assignments for any field whose safe default differs from zero (see lesson above).

---

## JVM-only plugin with flat CLI-arg interface: bridge to HTTP via arg parsing

**Category:** Plugin integration — `StableDiffusionBackend` / HTTP

`StableDiffusionBackend.generateImage(args: List<String>)` speaks CLI flags (produced by the
executor's `buildImageArgs`). When the actual generation runs in a Docker container via `server-sd`
(which takes JSON), an `HttpStableDiffusionBackend` must parse the flat arg list back into a
`GenerateExRequest` JSON body.

**Pattern:**
1. Maintain a `VALUE_FLAGS` set of all flags that take a value argument (`"--prompt"`, `"--steps"`, …).
2. Walk the arg list: if the current flag is in `VALUE_FLAGS`, consume the next token as its value.
3. Boolean/standalone flags (no value token) go into a `flags: Set<String>`.
4. Build the JSON body string directly from the parsed map.

**Gotcha:** `--seed -1` looks like a flag (`-1` starts with `-`). The `VALUE_FLAGS` approach handles
this correctly because we consume the next arg unconditionally when the flag is a known value-taker.
A `startsWith("-")` heuristic would misclassify `-1` as a new flag.

**Gotcha:** `--prompt` may appear multiple times (once per `<lora:path:mult>` lora entry, then once
for the real prompt). Collect all `--prompt` values; treat entries matching `<lora:...>` as lora
references, the last bare string as the actual prompt.

---

## stable-diffusion.cpp: empty backend string silently falls back to CPU via memory check

**Category:** JNI / server deployment — stable-diffusion.cpp backend selection

`GRAPHYN_SD_BACKEND: ""` (empty) triggers `EngineMemoryCheck.autoOffloadToCpu()`, which measures
**system RAM** (not GPU VRAM). Inside a Docker container this reads the container's memory limit,
not the host GPU. It almost always returns true → forces `"cpu"` → 750s per generation instead of
~37s on GPU.

**Rule:** Always set `GRAPHYN_SD_BACKEND: "cuda"` (or `"vulkan"` / `"metal"`) explicitly in
deployment config. Never rely on auto-detection in a containerised environment.

**Diagnostic signal:** `copy_to_backend: 0.00s` in every `model_loader.cpp` log line means tensors
are NOT being copied to the GPU backend. If you see this alongside `read: 15s`, the model is
streaming from disk into CPU RAM — GPU is idle. GPU inference shows meaningful `copy_to_backend`
times (several hundred ms to a few seconds per layer batch).

**Fixed in:** `server-sd/docker-compose.dev.yml` — changed `GRAPHYN_SD_BACKEND: ""` to
`GRAPHYN_SD_BACKEND: "cuda"`.

---

## `GraphynBootstrapJvm.mediaRuntimePlugins` is the correct place for JVM-only runtime plugins

**Category:** App architecture — plugin registration

JVM-only runtime plugins (those whose module is under `src/main/kotlin`, not `src/commonMain/kotlin`)
cannot be registered in `GraphynDemoPlugins` (in `commonMain`). They belong in
`GraphynBootstrapJvm.mediaRuntimePlugins` (`jvmMain`). This list is already merged into the
`runtimePlugins` call in `desktopApp/main.kt` at line 27 — adding a plugin here is all that's needed.

No editor plugin is required for `StableDiffusionPlugin` because it has no custom canvas card; the
standard `FieldCard` renders its specs.

---

## Split config nodes into path-only and compute-only for composability

**Category:** Plugin design — node modularity

A config node that owns both file paths and hardware settings (50+ ports) is hard to reuse and
impossible to swap independently. The `sd.context` node had this problem: model paths (24 ports)
and hardware compute options (27 ports) in one node. The fix is to split them: one `sd.model` node
owns only file paths and outputs an opaque token; `sd.context` takes that token plus compute ports
and merges them into the final context token.

In the executor for the aggregating node, strip `_type` from the upstream token's fields before
merging, so the downstream `_type` sentinel is always correct:
```kotlin
val modelFields = (inputs["model"] as? WorkflowValue.RecordValue)
    ?.fields?.minus("_type") ?: emptyMap()
mapOf("context" to SdTokens.context(modelFields + inputs.minus("model")))
```

Port lists were already separated in code (`sdContextPathPorts`, `sdContextComputePorts`) — the
only work was adding the `sd.model` spec and updating the executor.

---

## Generation node convenience output: `image` alongside `images`

**Category:** Plugin design — output ergonomics

A generation node that only outputs `images: ListType(StringType)` forces every single-image
workflow to use a list-index node to extract `images[0]`. Instead, expose both:
- `images: ListType(StringType)` — full batch (always set)
- `image: NullableType(StringType)` — `images.firstOrNull()` (convenience for single-image workflows)

This lets the FLUX workflow connect `txt2img.image → preview.value` directly, and multi-batch
workflows can still use `images`. The output change is additive and doesn't break existing graphs.

---

## Use `expect/actual` to inject platform-specific rendering into a common card

**Category:** Compose Multiplatform — preview plugin

`preview.view` renders text on all platforms but should show a real image on JVM Desktop when
the value is a file path. The fix: extract the content area into an
`internal expect fun PreviewContentArea(value: WorkflowValue?)`, then provide a JVM actual that
detects image extensions and calls `loadImageBitmap(FileInputStream(path))` + `Image(bitmap, ...)`.
All non-JVM actuals delegate to the text display.

Key: remove the `padding` from the container Box so the JVM actual can choose full-bleed image
vs. padded text independently. The image actual calls `Modifier.fillMaxSize()` + `ContentScale.Fit`
for a thumbnail; the text actual wraps itself in `Box(padding(8.dp))`.

---

## Group optional generation features into opaque tokens to shrink shared port lists

**Category:** Plugin design — node modularity

`imageGenSharedPorts` originally had 25 ports, including 11 that are only relevant when using
ControlNet, PhotoMaker, PuLID, or reference images. These are never wired in a basic txt2img
workflow. Grouping them into two optional opaque tokens reduces the node to 16 ports:

- `sd.controlnet` — control_image, control_strength, mask_image → `controlnet: NullableType(OpaqueType)`
- `sd.id_cond` — ref_images, auto_resize_ref_image, increase_ref_index, pm_*, pulid_* → `id_cond: NullableType(OpaqueType)`

The executors for the new config nodes wrap their inputs as typed records (`SdTokens.controlNet`/
`SdTokens.idCond`). The `buildImageArgs` function checks `NullValue` before calling the builder,
matching the existing pattern for `hires`, `cache`, `vae_tiling`. The port list physically
shrinks: moved port lists live in the new spec files, not in the shared list.

---

## SD plugin: split text encoders and VAE into dedicated nodes

**Category:** Plugin design — KMP node graph modularisation

`sd.model` started with 24 ports covering model paths, all encoder paths, and VAE config. Most
of these are never changed together: FLUX users set encoder paths once and never touch VAE format;
SD1.5 users often swap VAE without touching encoders. Grouping them into optional opaque tokens
reduces `sd.model` to 14 ports with two composable sub-nodes:

- `sd.encoders` — clip_l, clip_g, clip_vision, t5xxl, llm, llm_vision → `encoders: NullableType(OpaqueType)`
- `sd.vae`      — vae_path, vae_format (EnumType), audio_vae_path, taesd_path → `vae: NullableType(OpaqueType)`

The `sd.model` executor merges upstream tokens by stripping `_type` from each record before
combining, same pattern used by `sd.context` when it merges the `sd.model` token.

**Port type fixes applied in the same pass:**
- `backend` / `params_backend` in `SdContextComputePorts` changed from `NullableType(StringType)` → `NullableType(EnumType(SD_BACKENDS))` so the UI shows a picker instead of a free-text field.
- `max_vram` changed from `NullableType(StringType)` → `NullableType(DoubleType)` — the value is a GiB number; the old StringType accepted `"4"` but broke type-checking and number validation.
- `wtype` in model paths changed from `NullableType(StringType)` → `NullableType(EnumType(SD_WEIGHT_TYPES))` for the same reason.

**Rule:** whenever a node accumulates more than ~10 ports, check for semantic groups that are never
configured simultaneously (encoders vs model vs VAE). Each group that meets that criterion becomes
its own config node outputting an opaque token, and the parent node takes optional opaque inputs.

---

## OpaqueType wildcard survived in `completeConnection` after `WorkflowTypeCompatibility` fix

**Category:** Port type system — connection validation

`WorkflowTypeCompatibility.isCompatible` was fixed to make `OpaqueType` only match `OpaqueType`
(not everything), but `GraphynEditorConnectionActions.completeConnection` had its own redundant guard:

```kotlin
val compatible = outputType is WorkflowType.OpaqueType || inputType is WorkflowType.OpaqueType
    || WorkflowTypeCompatibility.isCompatible(inputType, outputType)
```

The first two conditions bypassed the fixed compatibility check, allowing any output to connect to
an OpaqueType input and any OpaqueType output to connect to any input.

**Fix:** Replace the entire expression with `WorkflowTypeCompatibility.isCompatible(inputType, outputType)`.

**Rule:** Type compatibility must be enforced in exactly one place (`WorkflowTypeCompatibility`). Any
ad-hoc OpaqueType shortcut in connection handlers re-introduces the wildcard bug.

---

## Auto-layout uses fallback `NodeSize(280×180)` for nodes with no registered canvas card

**Category:** Canvas layout — node height calculation

`performAutoLayout` computes each node's size by resolving the node type in `canvasCards`. Nodes
without a registered canvas card (e.g., SD plugin nodes registered only via `registerNodeSpec`) fall
back to `GraphynCanvasMetrics.NodeSize = IntSize(280, 180)`. A node like `sd.context` with 29 input
ports actually renders at ~697dp tall (via the auto-created `FieldCardFactory`), so the 180dp fallback
caused every subsequent node to be placed on top of the `sd.context` card visually.

**Fix:** When `canvasCards.resolve(type)` returns null, fall back to `nodeSpecs.resolve(type)` and
compute the height from the spec's port counts using a temporary `FieldCardFactory`:

```kotlin
registry?.resolve(type)?.let { IntSize(it.nodeWidth, it.nodeHeight) }
    ?: nodeSpecs?.resolve(type)?.let { spec ->
        val f = FieldCardFactory(inputRows = spec.inputs.size, outputRows = spec.outputs.size)
        IntSize(f.nodeWidth, f.nodeHeight)
    }
    ?: GraphynCanvasMetrics.NodeSize
```

**Rule:** Node size for auto-layout must match what the canvas renderer actually produces. The canvas
renderer falls back to `FieldCardFactory(inputs.size, outputs.size)` — the layout must use the same
formula.

---

## `portColor` as a semantic channel for OpaqueType ports

**Category:** Port type system — OpaqueType discrimination

OpaqueType ports use `portColor` as a semantic channel identifier. Without color-level matching,
any node with a generic OpaqueType input (Branch, Map, Filter) appears in the picker and gets
highlighted when dragging from a specialized colored port like `COLOR_MODEL` from `sd.context`.

**Rule:** When both the source and candidate port are OpaqueType (bare or NullableType-wrapped),
require their `portColor` to match exactly (`null == null`, `COLOR_A == COLOR_A`, `null ≠ COLOR_A`).
Non-OpaqueType ports are unaffected.

**Enforcement points:**
- `PortCompatibility.opaqueColorsMatch` — shared object in `app:shared`; called by all three validation layers
- `GraphynNodePickerHelpers.compatiblePickerSpecs` — picker popup suggestions
- `GraphynInputPortDot` / `GraphynOutputPortDot` — port dot highlighting and click rejection
- `GraphynEditorConnectionActions.completeConnection` — final server-side guard

**Fix:** Extract `PortCompatibility` object; replace all per-site `WorkflowTypeCompatibility.isCompatible` calls with `PortCompatibility.isCompatible` which chains type + color checks.

---

## SD plugin: one `portColor` constant for all OpaqueType channels defeats semantic separation

**Category:** SD plugin — portColor channel design

`SdNodeColors.kt` originally declared a single `COLOR_MODEL` constant and applied it to every
OpaqueType port in the plugin (diffusion, encoders, vae sub-tokens; the assembled model token;
the context token; controlnet; id_cond). With portColor-strict matching enabled this made every
OpaqueType port in the plugin mutually compatible — you could accidentally wire `sd.diffusion`
directly into `sd.txt2img.context`, bypassing `sd.model` and `sd.context` entirely.

**Rule:** Each semantic channel needs its own color constant. Channels in the SD plugin:

| Constant | Channel | Wire direction |
|---|---|---|
| `COLOR_DIFFUSION` | diffusion model paths sub-token | sd.diffusion → sd.model.diffusion |
| `COLOR_ENCODERS`  | encoder paths sub-token | sd.encoders → sd.model.encoders |
| `COLOR_VAE_PATH`  | VAE config sub-token | sd.vae → sd.model.vae |
| `COLOR_MODEL`     | assembled model paths token | sd.model → sd.context.model |
| `COLOR_CONTEXT`   | initialized context token | sd.context → sd.txt2img/img2img/txt2vid/img2vid.context |
| `COLOR_CONTROLNET`| ControlNet config token | sd.controlnet → generation.controlnet |
| `COLOR_ID_COND`   | id-conditioning token | sd.id_cond → generation.id_cond |

**Fix:** Add six new color constants to `SdNodeColors.kt`; update each spec file so output and
matching input use the same channel-specific constant.

---

## `GraphynLogPanel` auto-expand on run caused tall cards to appear shorter

**Category:** Compose layout — panel expansion, canvas viewport

`GraphynLogPanel` had `LaunchedEffect(result) { if (result != null) { expanded = true; ... } }`
which auto-opened the log panel on every run completion. The panel can grow up to ~274dp
(34dp header + 240dp body). The canvas area uses `Box(weight=1f)` above the log panel, so
when the log panel expands the canvas shrinks by the same amount. A 697dp tall node like
`sd.context` that filled the visible canvas suddenly overflows the bottom fold — visually
appearing shorter even though its rendered height didn't change.

**Rule:** Never auto-expand a bottom panel in response to state changes when the panel is
in-flow with a canvas area. The canvas already has `GraphynJobBadge` (top-right pill) to
surface run status. Remove the auto-expand so the canvas height stays stable. Users can
expand the log panel manually by clicking the header tabs.

**Fix location:** `GraphynLogPanel.kt` — removed the `LaunchedEffect(result)` block.

---

## `StepperChip` fixed width pushed `+` away from the card right edge

**Category:** Compose layout — row weight, chip sizing

`StepperChip` used `Modifier.width(VALUE_DP.dp)` (100dp fixed). Inside a `FieldRow` whose
`hasValue = true` adds a `Spacer(weight=1f)` before the chip, the chip starts at
`row_width − 100dp` from the left. The `+` button sits at the chip's right edge which IS
at the row right edge — but only when `currentValue != null` for the hasValue condition.
When `currentValue == null`, `hasValue = false` → no spacer → chip left-aligns after the
name with `+` nowhere near the right edge. More importantly, as the chip was fixed-width
regardless of the label name length, there was dead space and the chip appeared misaligned
against cards with wider/narrower labels.

**Fix:** Pass `hasValue = false` unconditionally in `NumericRow` (removing the spacer
from `FieldRow`), then pass `Modifier.weight(1f)` on `StepperChip` from the `RowScope`
content lambda. The chip fills remaining width from after the name label to the row right
edge. Inside `StepperChip`, the center value box uses `Modifier.weight(1f)` instead of
`widthIn(min = ...)` so it expands between the `−` and `+` buttons. The `+` is always
at the row right edge = card content right edge.

**Fix location:** `FieldCardStepper.kt` — `NumericRow`, `StepperChip`.

---

## `weight(1f)` for StepperChip causes inconsistent widths across rows

**Category:** Compose layout — chip sizing, FieldRow

Using `Modifier.weight(1f)` on `StepperChip` inside `FieldRow` makes each chip fill the
remaining space after the name label. Because name labels have different widths
("txt_cfg" vs "distilled_guidance"), the chip width varies per row — visually jarring.

**Rule:** Use a fixed `Modifier.width(VALUE_DP.dp)` (100dp) on `StepperChip` so all
chips are the same width. Pair it with `hasValue = currentValue != null` on `FieldRow`
so the `Spacer(weight=1f)` pushes the chip to the right edge only when there's something
to show. Inside `StepperChip`, the center value box can still use `Modifier.weight(1f)`
because it's constrained within the fixed-width chip Row.

**Fix location:** `FieldCardStepper.kt` — `NumericRow` (`hasValue`, `width(VALUE_DP.dp)`),
`StepperChip` (outer modifier fixed width, center box weight(1f)).


---

## List-port fan-in: wiring N single values into one `ListType` input

**Category:** graph engine — list ports (core/model + core/execution)

`sd.txt2img`/`img2img`/`txt2vid`/`img2vid` expose a `loras` input typed
`NullableType(ListType(OpaqueType))`; `sd.lora` outputs a single `OpaqueType` token. Originally
three independent rules made wiring `sd.lora → loras` non-functional, and the workaround was the
inline `<lora:name:weight>` prompt syntax. The graph engine now supports list-port fan-in
directly, so the `sd.lora` *node* works. Three coordinated changes were required — all three are
needed; fixing fewer leaves it broken:

1. **Type compatibility** (`WorkflowTypeCompatibility`) — the `ListType` branch now also accepts a
   single element-compatible source: `isCompatible(ListType(E), A)` is true when `isCompatible(E, A)`.
   So `Opaque → List<Opaque>` validates (a single value fans into the list).
2. **Fan-in allowed for list ports** (`validateConnections`) — `duplicate_input_connection` is no
   longer raised when the target input port resolves to a list type (`WorkflowType.listElementType()
   != null`). When the target node's *spec is unknown* (e.g. validating an SD workflow with only the
   demo plugins installed), the duplicate check is skipped entirely — `unknown_node_type` already
   covers it, and we can't classify an unresolved port.
3. **Runtime aggregation** (`buildInputMap`) — connections into a list port are collected into one
   `WorkflowValue.ListValue`. A single upstream that already emits a `ListValue` is passed through
   as-is; otherwise the gathered single values are wrapped into a list. Scalar ports keep
   last-write-wins as before.

Helper: `WorkflowType.listElementType()` unwraps a `NullableType` and returns the element type if
the port is a list, else null. Used by both the validator and the scheduler. Covered by
`ListPortFanInTest` (multiple + single connection → `ListValue`).

**Note:** every SD workflow (including FLUX) still reports ~37 non-blocking validation warnings
because the SD config specs mark many nullable ports `required`; these surface in the editor but
don't block execution. `DemoSceneWorkflowTest` exempts `sd.*` types (JVM-only media plugin) the
same way it exempts `media.*` and `script.eval`.

---

## Model paths flow from the workflow to the SD server (no more env reconfiguration)

**Category:** SD plugin · server-sd — per-request model loading

The thin `server-sd` originally loaded one model at boot from `GRAPHYN_SD_MODEL`/etc. env vars and
ignored per-request model paths — so switching models meant editing env vars and restarting, and the
editor's per-node model fields were decorative. The workflow already emits every model path as a CLI
flag (`--diffusion-model`, `--clip_l`, `--t5xxl`, `--vae`, `--llm`, `--high-noise-diffusion-model`,
…), but `argsToJson` dropped them and the server DTO had no fields for them.

**Fix (both halves):**
- **Client** (`SdArgsParser`): the model flags are now in `VALUE_FLAGS` and forwarded into the
  generate/generate-video JSON. They're server-side paths (models live on the server), so no upload.
- **Server**: `GenerateExRequest`/`GenerateVideoRequest` carry the model paths; `SdEngineCache` holds
  one `JniStableDiffusion` keyed on its `StableDiffusionConfig` and reloads only when the requested
  model changes (one model fits in VRAM at a time). Env vars are now just the fallback default. The
  Wan video route resolves model paths from the request, falling back to `GRAPHYN_WAN_*`.

**Gotchas:**
- LoRAs are applied by **full path** in `generateEx`; the native context init has no `lora_model_dir`
  parameter, so bare names + `lora_model_dir` do not resolve. Demo workflows use absolute LoRA paths.
- `StableDiffusionConfig` only covers the basic init fields (diffusion/clip_l/clip_g/t5xxl/vae/llm/
  fa/backend/threads) — enough for FLUX and Qwen. Split-checkpoint `--model`, `vae_format`, `taesd`,
  etc. would need the `initEx` path threaded too.
- `diffusionFa` defaults true for a reason (big MMDiT models spill VRAM without it); the client sends
  it as the presence of `--diffusion-fa`/`--fa`, so a workflow that wants it must set the context
  node's `diffusion_flash_attn = true`.

---

## Low-VRAM SD: it's a setting (max_vram offload), not a dedicated workflow

**Category:** SD plugin · server-sd — VRAM / RAM offload

A model larger than GPU VRAM (e.g. Qwen-Image Q4_K_M ~13 GB on a 12 GB card) crashed the native
sampler. The fix is *not* duplicate "low-VRAM" workflows — VRAM fit is a hardware property. The
`sd.context` node already exposes `max_vram`, `stream_layers`, `offload_params_to_cpu`, etc.

Three things had to line up; missing any one re-breaks it:
1. **Image engine must use `initEx`, not the basic `nativeInit`.** Only `initEx` accepts `max_vram`.
   The basic init silently ignored it, so graph-cut never happened. (The Wan engine already used the
   extended init — that's why 13.6 GB Wan fit but 13 GB Qwen didn't.) An empty `max_vram` maps to
   `nullptr` natively (full-VRAM, old behavior); `"-1"` = auto graph-cut.
2. **The client must forward `--max-vram`/`--stream-layers`**, and the server defaults `maxVram` to
   `"-1"` so offload is on by default.
3. **The container RAM limit must exceed the model's CPU-offload working set.** This was the real
   trap: with `limits.memory: 12g`, `EngineMemoryCheck` saw only 12 GiB RAM, decided the 12.6 GiB
   model needed *full* CPU offload (`backend=cpu`), which **disables** `max_vram` graph-cut
   (`--max-vram < 0 ... main backend is CPU; disabling graph splitting`), then loaded 18.8 GiB into
   a 12 GiB-capped container → OOM-kill → "server prematurely closed the connection". Raising to
   `24g` (host has 31) keeps it on CUDA + graph-cut.

**Result:** Q4_K_M Qwen (13 GB) runs on a 12 GB RTX 5070 — `graph cut max_vram=9905MB`, ~159 s for a
4-step 512² image. Smaller quants (Q2_K, ~41 s) remain a per-workflow speed/quality choice via the
model path. Diagnose offload issues from the server log lines `graph cut max_vram=` (good) vs
`disabling graph splitting` + `Enabling CPU param offload` (RAM-starved).

---

## Qwen-Image-Edit needs vision weights; SD elapsed is model-load-dominated

**Category:** SD models — img2img editing, benchmarking

Empirical results testing the AI workflows via the server-sd API (RTX 5070, 12 GB):

- **Text→Image works across tiers.** FLUX.1-schnell ~32 s (6.4 GB, fits VRAM, fastest), Qwen-Image
  Q2_K ~179 s, Q4_K_M ~159 s. The Qwen times are **model-load-dominated**, not compute — each tier
  swaps the cached model, forcing a 7–13 GB reload from disk. Re-running the *same* model reuses the
  SdEngineCache and is far faster. Report load vs sample separately when benchmarking.
- **Qwen-Image-Edit (img2img) is broken without vision weights.** With only the Qwen2.5-VL text GGUF
  and no mmproj/vision file, sd.cpp logs `no vision weights detected, vision disabled` and the edit
  can't condition on the input image — output is a green-tinted subject surrounded by a tiled mosaic
  of fragments (classic "vision off" artifact), not an edit of the source. Qwen-Edit requires its
  vision/mmproj projector to be present; otherwise prefer a single-stream editor.
- **Recommended swaps:** image editing → **FLUX.1-Kontext-dev** (instruction editor, no separate
  vision encoder, ~12B, faster than Qwen-Edit's ~20B). Fast video → **Wan2.2-TI2V-5B** (already on
  the server, single model vs the A14B MoE pair) or **LTX-Video** for near-real-time i2v.

---

## CORRECTION: Qwen-Image-Edit was not "unusable" — it needed --qwen-image-zero-cond-t

The earlier conclusion that Qwen-Image-Edit is broken on this server was wrong. Per the official
sd.cpp docs (docs/qwen_image_edit.md), **Qwen-Image-Edit 2511 requires `--qwen-image-zero-cond-t`;
without it the edit degrades badly** — that was the tiled-mosaic output, not a missing vision
encoder. Our pipeline emitted the flag from the context node but dropped it at three layers
(client argsToJson, server request DTO, and the JNI initEx call, which I'd only wired for the basic
fields). After threading `qwenImageZeroCondT` (and `llmVisionPath`) through all three, the same edit
produces a clean result.

Takeaway: don't declare a model "unusable" from one bad output — check the model's required flags
first. For best structure preservation Qwen-Edit also wants the Qwen2.5-VL mmproj (`--llm_vision`,
from mradermacher/Qwen2.5-VL-7B-Instruct-GGUF); the appearance/VAE path works without it but the
semantic path needs it.

## Wan A14B "OOM" on a 12 GB card is actually a throughput failure, not a fit failure

**Category:** Stable Diffusion — VRAM offload / model tiering

With `max_vram` graph-cut offload, the Wan 2.2 **A14B** i2v model (two-stage high+low-noise, ~25.8 GB unoffloaded) *does* fit a 12 GB card — it never OOMs. But every diffusion stage streams the 9.8 GB of offloaded weights from RAM per step, so throughput collapses: an 832×480×81 run logged `encode_first_stage` at **394.8 s** and `generate_video` stages in the hundreds of seconds each — a full run is 30–60 min. The dense **TI2V-5B** (~6 GB, fits natively) is the only practical i2v tier on 12 GB.

**Rule:** "Fits after offload" ≠ "usable." Tier a model by measured throughput, not just whether it OOMs. Keep A14B out of the automated template suite.

## Wan i2v VAE decode on 12 GB: a huge decode buffer + a known sd.cpp tiling bug (not a simple OOM)

**Category:** Stable Diffusion — Wan video VAE decode / stable-diffusion.cpp

Both Wan A14B and TI2V-5B i2v renders fail at VAE decode with `vae decode compute failed while processing a tile` → `decode_first_stage failed for video`. Two compounding causes, confirmed by research (leejet/stable-diffusion.cpp), not by guessing:

1. **The Wan VAE decode compute buffer is enormous** — ~12 GB @ 640×360, ~20.9 GB @ 832×480 (sd.cpp discussion #868). It exceeds a 12 GB card's headroom at real resolutions.
2. **Wan 2.2 VAE *tiling* — the escape hatch — is broken in sd.cpp** (issue #1284): tiled decode asserts/fails, which is exactly the "while processing a tile" error. So you cannot tile your way under the limit, and shrinking frames/resolution doesn't rescue it (it still tries to tile, and still fails).

The models themselves are compatible (correct VAE pairing: TI2V-5B→Wan2.2 VAE, A14B→Wan2.1 VAE; correct umt5 + clip_vision). On 12 GB the only working paths are `--vae-on-cpu` (~27 min/2 s clip) or the TAE tiny decoder (A14B only, not 5B) — both impractical.

**Rule:** When a native error mixes "compute failed" with "tile," check *both* the buffer size (is it an OOM the tiler should absorb?) *and* whether tiling is even functional for that model in that build — don't stop at "not OOM." The real fix is more VRAM (≥24 GB fits the untiled decode); see `runpod-serverless-plan.md`. Image tiers (FLUX + Qwen txt2img, Qwen-Edit img2img) all render fine on 12 GB.

## A blocking native generate monopolizes the single-model engine — order tests fast-first

**Category:** Testing — shared SD engine

`SdEngineCache` holds one model; a `generateEx` call is blocking C++ that a client TCP disconnect can't cancel. When JUnit scheduled `wanA14bImg2Vid` first, the abandoned ~1 hr A14B job kept the engine busy and every queued image request timed out client-side (the server still produced the image, but the client had given up — so nothing was saved). Run SD templates **individually, fast-first** (`--tests …fluxTxt2Img` etc.), never as one unordered class, so a slow video model can't block the image tier. There is no in-flight cancel — only draining or a container restart frees a stuck engine.
