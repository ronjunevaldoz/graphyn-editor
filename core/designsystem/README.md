# core/designsystem

Graphyn design tokens — colors, typography, spacing, and component primitives shared across all UI modules.

## What's here

- `GraphynDs` — single access point for all tokens (`GraphynDs.colors`, `GraphynDs.type`, `GraphynDs.spacing`)
- `GdsText`, `GdsIcon` — base text and icon primitives
- Theme presets (`GraphynThemePreset`) and appearance state (`GraphynAppearanceState`)

## Rules

- No Compose Material dependency — custom token system only
- All color values are resolved through `GraphynDs.colors`; never hardcode hex values in UI modules
- This module is a dependency of `app/shared`, `editor-api`, and all plugin UI modules
