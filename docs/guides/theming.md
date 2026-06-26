# Theming

Graphyn ships with a dark and a light colour scheme, four palette presets, and a `GraphynBranding` API for custom colours and logos.

---

## Quick start

Pass `GraphynBranding` to `GraphynEditorShell` or `GraphynApp`:

```kotlin
GraphynEditorShell(
    branding = GraphynBranding(
        appName = "My Workflow Editor",
        palette = GraphynThemePresets.Violet,
    ),
    dependencies = ...,
    state = state,
)
```

---

## Dark / light mode

`GraphynTheme` follows the system setting by default. Override with `rememberGraphynAppearanceState`:

```kotlin
val appearanceState = rememberGraphynAppearanceState()

// Force dark
appearanceState.themeMode = GraphynThemeMode.Dark

// Force light
appearanceState.themeMode = GraphynThemeMode.Light

// Follow system (default)
appearanceState.themeMode = GraphynThemeMode.System
```

---

## Palette presets

Four built-in palettes:

| Preset | Accent colour |
|---|---|
| `GraphynThemePresets.Default` | Coral `#FF6D5A` |
| `GraphynThemePresets.Violet` | Violet `#8B5CF6` |
| `GraphynThemePresets.Emerald` | Emerald `#10B981` |
| `GraphynThemePresets.Coral` | Warm coral (legacy n8n-inspired) |

```kotlin
GraphynBranding(palette = GraphynThemePresets.Emerald)
```

---

## Custom palette

`GraphynThemePreset` is a data class — override individual colours:

```kotlin
val myPalette = GraphynThemePresets.Default.copy(
    darkAccent  = Color(0xFF3B82F6),  // blue accent in dark mode
    lightAccent = Color(0xFF2563EB),  // blue accent in light mode
)

GraphynBranding(palette = myPalette)
```

---

## Design token reference

The full palette of semantic tokens used inside the editor is in `GraphynDsColors`. The dark and light variants are `GraphynDsColors.Dark` and `GraphynDsColors.Light`. These are derived from the active `GraphynThemePreset` at runtime — you do not normally need to override them directly.

| Token | Dark value | Light value | Used for |
|---|---|---|---|
| `canvasBackground` | `#1A1B26` | `#EEF0F5` | Canvas surface fill |
| `panelBackground` | `#252630` | `#FFFFFF` | Side panels, toolbar |
| `surfaceCard` | `#2D2E3D` | `#FFFFFF` | Node cards |
| `textPrimary` | `#E0E0E6` | `#1A1B26` | Labels, node titles |
| `textSecondary` | `#9B9BA5` | `#6B7280` | Port names, hints |
| `accent` | `#FF6D5A` | `#F55A4A` | Selected state, run button |
| `portInput` | `#7AA2FF` | `#4F7CFF` | Input port dots |
| `portOutput` | `#FF6D5A` | `#F55A4A` | Output port dots |
| `selectionRing` | `#7AA2FF` | `#4F7CFF` | Node selection border |
| `danger` | `#FF6B6B` | `#DC2626` | Error badge, delete actions |

Access the active colours inside a composable:

```kotlin
val colors = GraphynDs.colors
Box(Modifier.background(colors.surfaceCard)) { ... }
```

---

## Typography

The editor uses a single `GraphynDsTypography` instance with two weights. Override via branding if needed (future API — not yet exposed on `GraphynBranding`).

---

## Embedding inside an existing theme

`GraphynTheme` is a self-contained `CompositionLocal` scope. Wrap only the editor portion — your app's own theme continues to apply outside:

```kotlin
MyAppTheme {
    // Your app UI here

    GraphynTheme(branding = myBranding, darkTheme = darkTheme) {
        GraphynEditorShell(...)
    }
}
```
