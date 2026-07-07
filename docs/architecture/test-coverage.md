# Test Coverage Matrix

All tests are green as of the port-positioning / Phase 4 / port z-order commits.

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
| **Editor — auto-layout: diamond dependency does not overlap** | ✅ | | | |
| **Editor — auto-layout: wide branching tree stays non-overlapping** | ✅ | | | |
| **Editor — auto-layout: cyclic graph still lays out without hanging** | ✅ | | | |
| **Editor — auto-layout: isolated nodes grid-placed below DAG** | ✅ | | | |
| **Editor — auto-layout: barycenter sweep reduces edge crossings vs BFS order** | ✅ | | | |
| **Editor — draft connection → workflow connection** | ✅ | | | |
| **Editor — execution result applied to state** | ✅ | | | |
| **Editor — delete selected connection** | ✅ | | | |
| **Editor — reconnect connection replaces target** | ✅ | | | |
| **Editor — reconnect no-op when none selected** | ✅ | | | |
| **Editor — begin connection from input port sets isFromInput** | ✅ | | | |
| **Editor — complete from-input connection swaps endpoint order** | ✅ | | | |
| **Editor — cancel draft clears connectionDraft** | ✅ | | | |
| **Editor — delete key deletes selected node** | ✅ | | | |
| **Editor — delete key deletes selected connection** | ✅ | | | |
| **Editor — ShowNodePicker stores picker state with draft intact** | ✅ | | | |
| **Editor — DismissNodePicker clears draft and picker state** | ✅ | | | |
| **Editor — AddNodeAndConnect adds node and creates connection** | ✅ | | | |
| **Canvas — clicking empty canvas while draft shows node picker** | | ✅ | | |
| **Canvas — picking from node picker adds node and connects** | | ✅ | | |
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
| **Canvas — output port dot visible for node with spec** | | ✅ | | |
| **Canvas — input port dot visible for node with spec** | | ✅ | | |
| **Canvas — clicking output port starts connection draft** | | ✅ | | |
| **Canvas — connection midpoint dot visible when connection exists** | | ✅ | | |
| **Canvas — clicking midpoint dot selects connection** | | ✅ | | |
| **Canvas — empty nodes hint visible when workflow has no nodes** | | ✅ | | |
| **Canvas — reconnect via midpoint → input port click** | | ✅ | | |
| **Shell — clicking Auto Layout button repositions stacked diamond nodes** | | ✅ | | |
| **Shell — auto-layouted diamond (screenshot baseline)** | | ✅ | | 🖼 |
| **Shell — BFS layout button repositions nodes** | | ✅ | | |
| **Shell — BFS vs crossing-minimized layout (screenshot baselines)** | | ✅ | | 🖼 |
| **Canvas — node card renders (screenshot baseline)** | | | | 🖼 |
| **Canvas — port dots + connection midpoint (screenshot baseline)** | | | | 🖼 |
| **Canvas — full demo app renders (screenshot baseline)** | | | | 🖼 |
| **Plugin — SampleLogger installs and executes** | ✅ | | | |
| **Plugin — SampleLoggerUI registers panel** | ✅ | | | |
| **Demo — media workflow template wiring** | ✅ | | | |
| **Demo — media workflow execution (fake executors)** | ✅ | | | |

---

## Coverage gaps

| Feature | Status |
|---------|--------|
| Delete selected node (UI flow) | ⬜ state logic covered, no UI test |
| Port type validation rejection (UI click-through) | ⬜ unit covered, UI flow untested |
| Canvas — wasmJs port dot tests | ⬜ JVM only so far |

---

## Test file index

| File | Source set | Type |
|------|-----------|------|
| `core/.../CoreWorkflowTest.kt` | `commonTest` | Unit |
| `app/shared/.../SharedCommonTest.kt` | `commonTest` | Placeholder |
| `app/shared/.../EditorRegistryTest.kt` | `commonTest` | Unit |
| `app/shared/.../GraphynViewportMathTest.kt` | `commonTest` | Unit |
| `app/shared/.../GraphynAutoLayoutTest.kt` | `commonTest` | Unit |
| `app/shared/.../SharedLogicDesktopTest.kt` | `jvmTest` | Placeholder |
| `app/shared/.../MainKmpTest.kt` | `jvmTest` | Screenshot |
| `app/shared/.../GraphynNodeCardUiTest.kt` | `jvmTest` | Screenshot |
| `app/shared/.../GraphynNodeDragUiTest.kt` | `jvmTest` | UI |
| `app/shared/.../GraphynCanvasSurfaceUiTest.kt` | `jvmTest` | UI + Screenshot |
| `app/shared/.../GraphynEditorShellUiTest.kt` | `jvmTest` + `wasmJsTest` | UI |
| `app/shared/.../GraphynAutoLayoutUiTest.kt` | `jvmTest` | UI + Screenshot |
| `app/shared/.../GraphynAutoLayoutScreenshotTest.kt` | `jvmTest` | Screenshot |
| `app/shared/.../GraphynMinimapUiTest.kt` | `jvmTest` + `wasmJsTest` | UI + Screenshot |
| `plugins/sample-logger/.../SampleLoggerPluginTest.kt` | `commonTest` | Unit |
| `plugins/sample-logger-ui/.../SampleLoggerEditorPluginTest.kt` | `commonTest` | Unit |
| `app/app/.../MediaWorkflowTemplateTest.kt` | `commonTest` | Unit |
| `app/app/.../MediaWorkflowExecutionTest.kt` | `jvmTest` | Unit |
