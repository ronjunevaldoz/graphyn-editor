# Demo Scenarios — Design Analysis & Proposal

## Problem

The current demo workflow (two `sample.logger` nodes) demonstrates connectivity but nothing else.
`style-nodes` already defines three card styles (ShapeCard, FieldCard, CircleCard) and three
partial node specs (KSampler, DistributePoints, Webhook) — but no complete workflows that exercise
them. The result: the demo does not show what Graphyn actually looks like in a real use-case.

Three reference editors are in scope: **n8n**, **ComfyUI**, **Blender Geometry Nodes**.
Each has a distinct visual language and workflow type. Mixing them in one demo risks incoherence.

---

## Style Analysis

| | n8n | ComfyUI | Blender |
|---|---|---|---|
| **Card shape** | Wide rect + icon header | Rect + solid colour header | Rect + coloured title bar |
| **Port placement** | Fixed left / right anchors | Left inputs / right outputs | Left inputs / right outputs |
| **Port labels** | None on canvas | Labelled, always visible | Labelled, always visible |
| **Type encoding** | Minimal | Strong colour per type | Strong colour per type |
| **Inline config** | None — inspector only | Inline widgets (hide on connect) | Inline editable fields |
| **Sub-node concept** | Small circles (model, tool) | No | No |
| **Workflow domain** | Automation / API pipelines | AI image generation | 3D procedural geometry |
| **What makes it "complete"** | Trigger → transform → action | Source → sampler → decode → save | Primitive → modify → output |

### Graphyn card mapping

| Style | Graphyn card | Status |
|---|---|---|
| n8n wide card | `ShapeCard` | ✅ built |
| n8n sub-node circle | `CircleCard` | ✅ built |
| ComfyUI coloured header | `ShapeCard` (same shape, different colours) | ✅ built |
| Blender field list | `FieldCard` | ✅ built |
| Inline widget (ComfyUI / Blender) | ❌ not built | future |

---

## What's Missing

### Node specs
`style-nodes` has 3 partial specs. Complete workflows need:

**AI pipeline (ComfyUI):** CheckpointLoader, CLIPTextEncode, KSampler ✅, VAEDecode, SaveImage  
**Geometry pipeline (Blender):** MeshPrimitive, SubdivideMesh, DistributePoints ✅, InstanceOnPoints, GeometryOutput  
**Automation pipeline (n8n):** Webhook ✅, SetField, FilterIf, HTTPRequest, LogOutput

### Demo workflows
None of the three styles has a complete `WorkflowDefinition` wired end-to-end.

### Scene switching
No UI mechanism to switch between demo scenarios.

---

## Proposed Solution — Demo Scenes

One demo app, three named scenes. Each scene is a self-contained `WorkflowDefinition` that tells a
coherent story. A scene picker in the demo toolbar lets the user switch instantly.

### Why scenes, not one mixed workflow

- A mixed workflow forces incompatible visual languages into one canvas → looks broken
- Each scene can position nodes to show the flow direction clearly (left → right)
- Scenes can be extended independently as each style matures

### Scene 1 — AI Image Pipeline (ComfyUI aesthetic)

**Story:** Load a model, encode a prompt, sample latent space, decode to image, save.

```
CheckpointLoader → CLIPTextEncode (positive)  ─┐
                 → CLIPTextEncode (negative)  ─┤→ KSampler → VAEDecode → SaveImage
                 → (latent noise)             ─┘
```

- Card: `ShapeCard`  
- Port colours: model=blue-purple, conditioning=orange, latent=purple, image=green  
- New specs needed: `CheckpointLoader`, `CLIPTextEncode`, `VAEDecode`, `SaveImage`

### Scene 2 — Geometry Pipeline (Blender aesthetic)

**Story:** Create a mesh, subdivide it, scatter points on its surface, instance objects on points.

```
MeshPrimitive → SubdivideMesh → DistributePoints → InstanceOnPoints → GeometryOutput
```

- Card: `FieldCard`  
- Port colours: geometry=green, float=gray, int=dark-gray, vector=teal  
- New specs needed: `MeshPrimitive`, `SubdivideMesh`, `InstanceOnPoints`, `GeometryOutput`

### Scene 3 — Automation Pipeline (n8n aesthetic)

**Story:** Receive a webhook, extract a field, filter by condition, call an HTTP API, log the result.

```
Webhook → SetField → FilterIf → HTTPRequest → LogOutput
```

- Card: `ShapeCard` for data nodes, `CircleCard` for Webhook trigger and LogOutput  
- Port colours: data=neutral, boolean=yellow, string=blue  
- New specs needed: `SetField`, `FilterIf`, `HTTPRequest`, `LogOutput`

---

## Execution Strategy

All style-nodes are **visual-only** — register passthrough executors that forward inputs to outputs
unchanged. The Execute button still lights up status badges, demonstrating the execution UI without
requiring real model inference, geometry computation, or HTTP calls.

```kotlin
registrar.registerExecutor("stylenodes.ksampler") { inputs ->
    mapOf("latent" to (inputs["latent"] ?: WorkflowValue.NullValue))
}
```

---

## Implementation Plan

| Step | Work | Location |
|---|---|---|
| 1 | Add missing node specs (10 new) | `style-nodes/StyleNodesSpecs.kt` |
| 2 | Add passthrough executors for all style-nodes | `style-nodes/StyleNodesPlugin.kt` |
| 3 | Define 3 `WorkflowDefinition` scene objects | `app/demo/bootstrap/GraphynBootstrap.kt` |
| 4 | Add scene picker to demo toolbar | `app/demo` — new `DemoToolbarExtras.kt` |
| 5 | Wire scene switcher into `DemoApp` state | `app/demo/DemoApp.kt` |

Steps 1–3 are self-contained and can land first. Steps 4–5 are UI and can follow.

---

## What We Are NOT Doing

- **No inline widgets yet** — ComfyUI/Blender inline controls are a separate feature tracked elsewhere
- **No real executors** — style-nodes are demonstrations, not functional node implementations
- **No fourth style** — ReactFlow/Retool style is interesting but out of scope for 0.x
- **No separate plugin per style** — all three scenes live in `style-nodes` categorised by type prefix (`stylenodes.ai.*`, `stylenodes.geo.*`, `stylenodes.auto.*`)
