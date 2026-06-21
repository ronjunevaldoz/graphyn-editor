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

## Minimap Node Size `* 2f` Bug

**Category:** Minimap rendering — coordinate space

**Problem:** `GraphynMinimapDebugger.kt` multiplied node width/height by `* 2f` when drawing nodes in the minimap. This kept the top-left position correct but doubled the rendered size, shifting the visual center of each node rectangle to the actual node's bottom-right corner in world space. With accurate card sizes from `canvasCards`, the preview node's inflated rectangle clipped outside the viewport indicator even though the layout was mathematically centered.

**Fix and rule:** Use `maxOf(nodeW * minimapLayout.scale, 3f)` — proportionally correct size with a 3px minimum for visibility. Never multiply minimap node sizes by an arbitrary factor; it breaks the position/size relationship and makes the viewport indicator look wrong.