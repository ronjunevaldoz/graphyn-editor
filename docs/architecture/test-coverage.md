# Test Coverage Matrix

All tests are green as of the port-positioning / Phase 4 commits.

## Legend

| Symbol | Meaning |
|--------|---------|
| ✅ | Covered |
| ⬜ | Not yet covered |
| 🖼 | Screenshot / pixel assertion (Roborazzi) |

---

## Coverage by feature area

| Feature | Unit (common) | UI — JVM | UI — wasmJs | Screenshot |
|---------|:---:|:---:|:---:|:---:|
| **Core — type compatibility** | ✅ | | | |
| **Core — workflow serialization (data class)** | ✅ | | | |
| **Core — workflow JSON round-trip** | ✅ | | | |
| **Core — WorkflowValue flattening** | ✅ | | | |
| **Core — validator: missing required inputs** | ✅ | | | |
| **Core — validator: type-incompatible connections** | ✅ | | | |
| **Core — downstream node propagation** | ✅ | | | |
| **Core — execution topological order** | ✅ | | | |
| **Editor — panel registry stores panels** | ✅ | | | |
| **Editor — plugin registers panel** | ✅ | | | |
| **Editor — state tracks outputs & downstream impact** | ✅ | | | |
| **Editor — state tracks node positions + fallback layout** | ✅ | | | |
| **Editor — draft connection → workflow connection** | ✅ | | | |
| **Editor — execution result applied to state** | ✅ | | | |
| **Viewport — panBy moves offset** | ✅ | | | |
| **Viewport — zoomAt keeps focus stable** | ✅ | | | |
| **Viewport — minimap layout + viewport rect bounded** | ✅ | | | |
| **Viewport — minimap point recenters viewport** | ✅ | | | |
| **Viewport — hit test detects node bounds** | ✅ | | | |
| **Viewport — drag delta normalized by scale** | ✅ | | | |
| **Viewport — repeated small drags accumulate without drift** | ✅ | | | |
| **Viewport — world bounds stable on intra-frame node move** | ✅ | | | |
| **Viewport — world bounds use configured canvas frame** | ✅ | | | |
| **Viewport — transform clamped to canvas frame** | ✅ | | | |
| **Viewport — node positions clamped to canvas frame** | ✅ | | | |
| **Shell — zoom-in button updates viewport scale** | | ✅ | ✅ | |
| **Shell — registered panel renders for selected node** | | ✅ | ✅ | |
| **Shell — minimap viewport border visible (pixel assert)** | | ✅ | ✅ | 🖼 |
| **Canvas — dragging node does not pan viewport** | | ✅ | | |
| **Canvas — node card renders (screenshot baseline)** | | | | 🖼 |
| **Canvas — full demo app renders (screenshot baseline)** | | | | 🖼 |
| **Plugin — SampleLogger installs and executes** | ✅ | | | |
| **Plugin — SampleLoggerUI registers panel** | ✅ | | | |

---

## Coverage gaps (Phase 4 targets)

| Feature | Status |
|---------|--------|
| Delete selected node via intent | ✅ state logic exists, ⬜ no UI test |
| Delete selected connection | ⬜ not implemented |
| Reconnect existing connection | ⬜ not implemented |
| Port type validation rejection (UI flow) | ⬜ unit covered, UI flow untested |
| Empty canvas hint renders | ⬜ |
| Port drag hover states | ⬜ |

---

## Test file index

| File | Source set | Type |
|------|-----------|------|
| `core/.../CoreWorkflowTest.kt` | `commonTest` | Unit |
| `app/shared/.../SharedCommonTest.kt` | `commonTest` | Placeholder |
| `app/shared/.../EditorRegistryTest.kt` | `commonTest` | Unit |
| `app/shared/.../GraphynViewportMathTest.kt` | `commonTest` | Unit |
| `app/shared/.../SharedLogicDesktopTest.kt` | `jvmTest` | Placeholder |
| `app/shared/.../MainKmpTest.kt` | `jvmTest` | Screenshot |
| `app/shared/.../GraphynNodeCardUiTest.kt` | `jvmTest` | Screenshot |
| `app/shared/.../GraphynNodeDragUiTest.kt` | `jvmTest` | UI |
| `app/shared/.../GraphynEditorShellUiTest.kt` | `jvmTest` + `wasmJsTest` | UI |
| `app/shared/.../GraphynMinimapUiTest.kt` | `jvmTest` + `wasmJsTest` | UI + Screenshot |
| `plugins/sample-logger/.../SampleLoggerPluginTest.kt` | `commonTest` | Unit |
| `plugins/sample-logger-ui/.../SampleLoggerEditorPluginTest.kt` | `commonTest` | Unit |
