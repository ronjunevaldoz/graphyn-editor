# Architecture

Graphyn is being shaped as a library-first workflow editor with a separate editor shell.

Current boundaries:
- `core`: workflow types, node specs, ports, validation, serialization
- `editor`: canvas, node interaction, panels, and tool hosting
- `server`: execution/runtime service

Design notes:
- The core must not depend on Compose UI.
- Panels belong to the editor layer.
- Node registration should be registry-based instead of switch-based.

Related:
- [Type Model](./types.md)
- [Core API Draft](./core-api.md)
- [Plugin API Draft](./plugins.md)
- [Plans](../plans/README.md)
