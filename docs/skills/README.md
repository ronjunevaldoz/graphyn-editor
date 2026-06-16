# Skills Prospect

This document tracks future Codex/agent skill ideas that would help Graphyn stay consistent as the library grows.

The goal is not to replace the repo docs. The goal is to identify repeatable patterns that could later become reusable skills.

## Why this exists

Graphyn already has strong shared rules for:
- node specs
- executors
- registries
- plugin wiring

The remaining pain points are mostly UI and editor-shape decisions:
- canvas shell composition
- minimap behavior
- panel hosting
- modifier usage
- slot-based node UI structure

Those patterns are repeatable enough that they may eventually deserve their own skill guidance.

## Current skill gaps we keep hitting

- Editor shell composition becomes too large if the rules stay in one file.
- Canvas world-space math needs a stable reference model.
- Minimap layout, viewport bounds, and camera dragging need one shared source of truth.
- Modifier usage needs clearer intent guidance so behavior, transforms, and drawing stay in the right layer.
- Slot-based node composition needs a public pattern for custom panels and node layouts.
- Plugin registration is clear at the runtime layer, but UI extension points need the same clarity.

## Candidate future skills

### 1. Graphyn editor shell skill

Would cover:
- shell decomposition
- top toolbar
- palette
- inspector
- logs
- telemetry overlays
- minimap placement

Use case:
- whenever a new editor surface or panel is added
- when the shell starts to grow beyond orchestration

### 2. Graphyn canvas world-space skill

Would cover:
- viewport math
- pan/zoom behavior
- world-space drag rules
- node movement
- connection drawing
- bounds clamping

Use case:
- when adding or changing camera behavior
- when node dragging feels multiplied or offset

### 3. Graphyn minimap skill

Would cover:
- minimap fit rules
- camera box rendering
- drag continuity
- mapping between world and minimap
- fixed-size overlay behavior

Use case:
- when the minimap becomes a debugging surface
- when viewport and minimap disagree visually

### 4. Graphyn panel host skill

Would cover:
- default inspector panels
- custom node panels
- validation panel
- workflow settings panel
- extension registration for editor UI

Use case:
- when registering a new node-specific editor surface
- when adding custom plugin UI

### 5. Graphyn modifier guidance skill

Would cover:
- when to use modifiers vs composables
- how to structure reusable graphics modifiers
- when to use `graphicsLayer`
- when to use `Canvas`, `drawBehind`, or `drawWithCache`
- how to keep modifier extensions stateless

Use case:
- when UI behavior starts leaking into drawing code
- when a modifier becomes a mini framework

### 6. Graphyn node-slot skill

Would cover:
- named slots for `GraphynNodeCard`
- custom node layouts
- slot-based panel injection
- preserving a small public API surface

Use case:
- when a node needs custom header/body/footer content
- when we want to avoid hardcoded node card variants

## Rules for future skill design

- Keep the skill thin.
- Prefer references over duplication.
- Put implementation details in docs or code examples, not in giant rules files.
- Make the skill explain the decision boundary, not every possible code path.
- Treat the shared core as UI-agnostic.
- Treat editor UI as an extension layer.

## Suggested trigger ideas

- "build a new editor panel"
- "add a new canvas interaction"
- "fix minimap mapping"
- "add a reusable modifier"
- "convert a component to slot API"
- "register a custom node panel"

## Related docs

- [Agent Rules](../agents.md)
- [Architecture](../architecture/README.md)
- [Type Model](../architecture/types.md)
- [Plugin API Draft](../architecture/plugins.md)
- [Plan Phases](../plans/README.md)

