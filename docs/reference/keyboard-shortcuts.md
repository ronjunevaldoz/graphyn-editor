# Keyboard Shortcuts

All shortcuts use the primary meta key: **Cmd** on macOS, **Ctrl** on Windows and Linux.

---

## Canvas navigation

| Shortcut | Action |
|---|---|
| Scroll wheel | Zoom in / out |
| Click + drag (empty canvas) | Pan |
| `F` | Fit all nodes to screen |

---

## Selection

| Shortcut | Action |
|---|---|
| Click node | Select node |
| Click connection midpoint | Select connection |
| Click empty canvas | Deselect all |
| `Cmd + A` | Select all nodes |
| `Escape` | Deselect all / cancel draft |

---

## Editing

| Shortcut | Action |
|---|---|
| `Backspace` or `Delete` | Delete selected node or connection |
| `Cmd + C` | Copy selected nodes |
| `Cmd + V` | Paste copied nodes |
| `Cmd + D` | Duplicate selected nodes |
| `Cmd + G` | Group selected nodes (requires ≥ 2 selected) |
| `Cmd + Shift + G` | Collapse selected nodes into a subgraph (requires ≥ 2 selected) |

To expand a subgraph back into its nodes, select it and use **Expand ⤢** in the inspector.

---

## History

| Shortcut | Action |
|---|---|
| `Cmd + Z` | Undo |
| `Cmd + Shift + Z` | Redo |

---

## Execution

| Shortcut | Action |
|---|---|
| `Cmd + Enter` | Run workflow |

---

## Connection wiring

| Action | How |
|---|---|
| Start connection | Click an output (right-side) port dot |
| Start connection from input | Click an input (left-side) port dot |
| Complete connection | Click the matching port dot on another node |
| Cancel in-progress connection | Click empty canvas or press `Escape` |
| Auto-pick node on drop | Drop connection wire on empty canvas — node picker appears |
| Reconnect existing wire | Click the midpoint dot on an existing connection, then click a new port |

---

## Customising shortcuts

Shortcuts are defined in `GraphynShortcuts.kt` in the `app/shared` module. Each shortcut is a named `fun` on `GraphynShortcuts` that returns `Boolean` from a `KeyEvent`. Override the shortcut handler by subclassing `GraphynEditorState` and overriding the key handler. (A first-class `shortcutOverrides` API is planned for a future release.)
