# Node Design Research

Living reference for the Graphyn node card design. Updated as new editors are studied.

---

## Design axes

| Axis | Options |
|---|---|
| **Size** | Fixed · Content-driven · Icon-only |
| **Port visibility** | Always visible · Hover only · Inspector only |
| **Inline config** | None · Read-only preview · Editable controls |
| **Type encoding** | None · Accent colour · Port dot colour · Header colour |
| **Connection model** | Port-to-port labels · Positional (top/bottom/left/right) |

## Visual comparison

<iframe src="node-design-comparison.html" width="100%" height="620" style="border:none;border-radius:8px;"></iframe>

---

## Editor survey

### n8n (2 modes)

**Detailed card** (agent nodes — Meeting Availability Agent, Generate Message)
- Fixed wide card, icon + title + subtitle in header
- No port names on canvas — connections attach to fixed left/right anchors
- Sub-nodes attach below as small circles (Model, Availability, Output)
- Config lives entirely in the inspector side panel

**Monolithic circle** (sub-nodes — Model, Availability, Output)
- 48–56 px circle, icon only
- Label + 1-line config summary below the circle
- Connection dots on left/right of circle
- Best for nodes with 1-in / 1-out fixed arity

**Verdict:** Two separate components, not a flag on one. Works because n8n has a strict two-tier node hierarchy.

---

### Blender Geometry Nodes (studied 2026-06-18)

- Rectangle with **coloured title bar** (category colour)
- Port dots on left (inputs) and right (outputs) — always visible
- Port names printed next to each dot
- **Inline editable controls** directly on the node (number fields, dropdowns, toggles)
- Node width is content-driven — expands to fit the widest field
- Port dot colour encodes data type (green = geometry, teal = vector, grey = float, purple = boolean)
- No separate inspector — everything configurable on the canvas
- Collapsed mode: hides all ports except connections in use

**Verdict:** The richest data-type system of any editor studied. Port colour is load-bearing — users learn it fast. Inline controls reduce inspector round-trips but create very tall nodes on complex graphs.

---

### ComfyUI (studied 2026-06-18)

- Rectangle with **solid coloured header** (dark, category-coded)
- Port dots left (inputs) / right (outputs) — always visible with labels
- Some inputs render as **inline widgets** (sliders, text boxes, dropdowns) when no connection is attached — widget disappears when a wire is connected
- Node width is fixed per type, height is content-driven
- Port dot colour encodes data type: latent = purple, image = green, conditioning = orange, model = blue, clip = yellow, vae = pink
- No inspector panel — the node IS the config surface
- Nodes can be grouped / framed with a coloured annotation box

**Verdict:** The "inline widget that disappears on connect" pattern is excellent UX — it makes the node self-documenting and eliminates the inspector entirely for simple configs. The colour-coded port system is the clearest studied.

---

## Graphyn current design (as of 0.1.0)

- Fixed 280×180 card
- Left accent bar (single colour, not category-coded)
- Header: label + node ID
- Port columns: input names left, output names right (text badges, no dots on edges)
- Footer: live output preview + "Connecting…" hint
- Execution status badge top-right
- Config: inspector panel only

**Gaps identified vs. studied editors:**
1. Port dots are not on the card edge — connections must snap to card bounds, not named ports
2. No port type colour encoding
3. No inline widget support (config always requires inspector)
4. Fixed size means tall-port nodes clip or waste space
5. No collapsed / compact mode

---

## Recommendations backlog

| Priority | Change | Reference |
|---|---|---|
| High | Move port dots to card edges (left for inputs, right for outputs) | Blender, ComfyUI |
| High | Colour-code port dots by `WorkflowType` | ComfyUI |
| Medium | Add `compact: Boolean` to `NodeSpec` → render icon-circle variant | n8n sub-nodes |
| Medium | Content-driven card height (min 80 dp, grows with port count) | Blender, ComfyUI |
| Medium | Inline widget for unconnected scalar inputs (`WorkflowType.NumberType`, `StringType`) | ComfyUI |
| Low | Category colour on header bar instead of single accent | Blender, ComfyUI |
| Low | Collapsed mode (hides unconnected ports) | Blender |
