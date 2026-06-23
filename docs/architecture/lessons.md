# Engineering Lessons

General patterns discovered during development. Each entry is written to be reusable
across projects and to serve as raw material for agent skills.

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

**Never import `androidx.compose.material3.*` in `app/demo`.** If a new component needs
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
needed for a demo live as `WorkflowDefinition` data local to `app/demo`, not as registered
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

**Fix and rule:** Define demo-only node types (spec + runtime plugin + editor plugin) inside `app/demo/src/commonMain/kotlin/.../bootstrap/` as a single `*DemoPlugin.kt` file. Register them in `GraphynDemoPlugins.runtime` and `GraphynDemoPlugins.editor`. They live and die with the demo module. When a concept matures to production, extract it into a proper `plugins/` module.

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

**Fix and rule:** Custom cards (`NodeCanvasFactory` implementations) must overlay `NodeStatusBadge` (or `GraphynNodeStatusBadge`) and provide a jvmTest that checks badge text (`"+"`, `"v"`, `"x"`) for each `NodeExecutionStatus` value. `app/demo` can host its own `jvmTest` source set with `compose.desktop.uiTestJUnit4` — no roborazzi plugin needed for behavior-only tests. Pattern for the overlay: wrap the card in `Box(Modifier.size(...))` and add `NodeStatusBadge(ctx.executionStatus, Modifier.align(Alignment.TopEnd).padding(4.dp), surfaceColor = cardBg)`.


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

**Problem:** A JVM-only plugin (`plugins/script`) needs a custom `NodeCanvasFactory` with Compose. The plugin can't use `ui:cards` `internal` helpers, and `app/demo` is KMP so it can't host JVM-only UI.

**Fix and rule:** The JVM-only plugin (`kotlinJvm`) depends on `ui:cards` and `editor-api` and defines its own `NodeCanvasFactory` implementation (e.g., `ScriptCardFactory`). The editor plugin registers it via `registrar.registerCanvasCard(type, ScriptCardFactory)`. The KMP `app/demo` module uses the node type string `"script.eval"` with no import — it compiles on all targets. The JVM-only plugin is wired in `app/desktopApp/main.kt` which is already JVM-only.

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
Modules that depend on `:core` via `api(projects.core)` (e.g., `app/shared`, `app/demo`)
do not get `Clock.System` transitively — `implementation` deps are not exported.
Attempting to use `Clock.System.now()` in `app/demo/commonMain` fails to compile.

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
