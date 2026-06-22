# Code Audit — File Size

Files exceeding the 150-line ceiling defined in `CLAUDE.md`. Ordered by size descending.

| File | Lines | Primary concern | Suggested split |
|------|------:|-----------------|-----------------|
| `editor/state/GraphynEditorState.kt` | 135 ✅ split done | Coordinator: workflow mutations, selection, dispatch | Sub-states extracted: `GraphynNodeLayoutState` (82L), `GraphynViewportState`, `GraphynDebugLogState`; viewport fit/resize actions in `GraphynEditorViewportActions.kt` |
| `editor/canvas/GraphynCanvasSurface.kt` | 113 ✅ split done | Canvas orchestrator | Extracted: `GraphynCanvasGestures` (gestures), `GraphynConnectionMidpoints`, `GraphynNodePortDots` (port dots with hover + reconnect) |
| `editor/theme/GraphynTheme.kt` | 262 | Color schemes, typography, palette presets, appearance state | Split into `GraphynPalette.kt`, `GraphynColorScheme.kt`, `GraphynTypography.kt`, `GraphynAppearanceState.kt` |
| `editor/canvas/components/GraphynNodeCard.kt` | 237 | Card shell + drag + port columns + port bubbles + summary section | Extract `GraphynPortColumn` + `GraphynPortBubble` → `GraphynNodeCardPorts.kt`; extract `GraphynSummarySection` → `GraphynNodeCardSummary.kt` |
| `editor/shell/components/GraphynMinimapDebugger.kt` | 175 | Minimap draw + viewport rect + node rects + telemetry overlay | Split minimap Canvas draw logic into `GraphynMinimapCanvas.kt` |
| `editor/shell/GraphynEditorShell.kt` | 175 | Shell scaffold + panel layout + slot wiring | Shell layout is already thin; split only if panel wiring grows |

## Status key

| Tag | Meaning |
|-----|---------|
| 🔴 | Over limit, no plan yet |
| 🟡 | Over limit, split planned |
| 🟢 | Under 150 lines |

All files above are 🔴 as of Phase 4. Address `GraphynEditorState` and `GraphynCanvasSurface` first — they carry the most logic and are the most actively edited.
