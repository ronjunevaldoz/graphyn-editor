# Keyboard Shortcuts

All shortcuts use the primary meta key: **Cmd** on macOS, **Ctrl** on Windows and Linux.

**Configurable:** the shortcuts below are defaults. Open the **⌨ Keys** button in the toolbar to
rebind any action — click its chord chip, press the new combination, and it's saved. Conflicts are
rejected with a message; **Reset all** restores defaults. Bindings persist via the settings store.
Contextual keys (Escape, Delete/Backspace) are fixed and not remappable.

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

Double-click a subgraph node to drill in; use **Expand ⤢** in the inspector to inline it again.

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
