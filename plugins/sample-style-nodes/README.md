# plugins/style-nodes

Reference editor plugin demonstrating three custom canvas card styles.

## Card types

| Card | Description |
|---|---|
| `DarkHeaderCard` | Dark header with colored port dots — n8n-style |
| `FieldCard` | Field list layout with labeled input/output rows |
| `CircleCard` | Minimal circular node for simple pass-through nodes |

## Usage

```kotlin
// Runtime plugin (node specs)
registry.install(StyleNodesPlugin)

// Editor plugin (custom canvas cards)
editorRegistry.install(StyleNodesEditorPlugin)
```

## Port color dots

Port colors are driven by `PortSpec.portColor: Long?`. When set, the dot renders in that color; otherwise falls back to a neutral gray. See `StyleNodesSpecs.kt` for examples.

All cards support the `NodeStatusBadge` execution status indicator at `Alignment.TopEnd`.
