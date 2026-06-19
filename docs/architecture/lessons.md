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

`style-nodes` was built to demonstrate three card shapes (DarkHeaderCard, FieldCard,
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

Three card styles (`DarkHeaderCard`, `FieldCard`, `CircleCard`) drifted independently: different
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

## Dynamically-Sized Cards Must Set Their Own Size via `Modifier.size()`

**Category:** Compose layout — card sizing in unconstrained parents

**Problem:** Sticky note cards placed in `Box(Modifier.offset { pos })` are in an unconstrained container. `fillMaxSize()` inside an unconstrained Box resolves to 0×0, making the card invisible or a single-line label.

**Fix and rule:** Cards that need a fixed or user-controlled size must apply `Modifier.size(w.dp, h.dp)` explicitly. Store user-adjusted dimensions in node config (e.g., `__w` / `__h` keys); read them at render time and fall back to constants. Never rely on `fillMaxSize()` to fill an unconstrained canvas slot.

