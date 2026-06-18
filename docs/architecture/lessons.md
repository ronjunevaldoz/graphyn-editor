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
