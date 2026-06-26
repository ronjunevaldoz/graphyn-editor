# Compatibility Matrix

## Published artifacts

All artifacts share the same version and are published to [Maven Central](https://central.sonatype.com/search?q=io.github.ronjunevaldoz) under `io.github.ronjunevaldoz`.

| Artifact | Maven coordinate | Since | Description |
|---|---|---|---|
| `graphyn-core-model` | `io.github.ronjunevaldoz:graphyn-core-model` | 0.3.0 | Workflow graph types, type system, registry, validation |
| `graphyn-core-execution` | `io.github.ronjunevaldoz:graphyn-core-execution` | 0.3.0 | Execution engine and events |
| `graphyn-core-serialization` | `io.github.ronjunevaldoz:graphyn-core-serialization` | 0.3.0 | Workflow JSON codec |
| `graphyn-core-data` | `io.github.ronjunevaldoz:graphyn-core-data` | 0.3.0 | Workflow stores + platform persistence |
| `graphyn-plugin-api` | `io.github.ronjunevaldoz:graphyn-plugin-api` | 0.3.0 | Contracts for runtime node plugins |
| `graphyn-editor-api` | `io.github.ronjunevaldoz:graphyn-editor-api` | 0.3.0 | Contracts for editor canvas cards and panels |
| `graphyn-ui-cards` | `io.github.ronjunevaldoz:graphyn-ui-cards` | 0.3.0 | Built-in canvas card renderers (FieldCard, ShapeCard, CircleCard) |
| `graphyn-editor` | `io.github.ronjunevaldoz:graphyn-editor` | 0.3.0 | Compose Multiplatform editor shell, canvas, launcher |
| `graphyn-server` | `io.github.ronjunevaldoz:graphyn-server` | **0.6.0** | Ktor server + `install(Graphyn)` embeddable plugin |

> **`graphyn-server` first published in 0.6.0** — available on Maven Central as `io.github.ronjunevaldoz:graphyn-server:0.6.0`.

---

## Kotlin Multiplatform targets

| Artifact | JVM | Android | JS (browser) | WASM JS | iOS |
|---|:---:|:---:|:---:|:---:|:---:|
| `graphyn-core-model` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `graphyn-core-execution` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `graphyn-core-serialization` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `graphyn-core-data` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `graphyn-plugin-api` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `graphyn-editor-api` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `graphyn-ui-cards` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `graphyn-editor` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `graphyn-server` | ✅ | — | — | — | — |

`graphyn-server` is JVM-only — it is a Ktor server, not a KMP library.

---

## Plugin modules (not published — source only)

These ship in the repository and are depended on by the demo app. They are **not** on Maven Central; consumers copy or fork them.

| Module | JVM | Android | Common | Notes |
|---|:---:|:---:|:---:|---|
| `plugins/media-core` | ✅ | — | — | FFmpeg-backed video/audio/image ops. Requires FFmpeg on `PATH`. |
| `plugins/media-ai` | ✅ | — | — | TTS / STT / OCR adapters. Zero-config fallbacks: macOS `say`, `tesseract`. |
| `plugins/io` | ✅ | ✅ | ✅ | File read/write, HTTP, path resolution |
| `plugins/text` | ✅ | ✅ | ✅ | String ops, template rendering |
| `plugins/script` | ✅ | — | — | Kotlin Script (`script.eval`), JVM-only |
| `plugins/sticky-notes` | ✅ | ✅ | ✅ | On-canvas annotation nodes |
| `plugins/sample-style-nodes` | ✅ | ✅ | ✅ | Demo card shapes (ShapeCard, FieldCard, CircleCard) |

---

## Version history

| Version | Date | Highlights |
|---|---|---|
| 0.6.0 | 2026-06-26 | `graphyn-server` first publish; `install(Graphyn)` Ktor plugin; security: replaced hardcoded home-server URL with `GRAPHYN_OLLAMA_HOST` env var |
| 0.5.0 | 2026-06-26 | Phase 2 & 3 media nodes; production app reframe; platform-gated catalog |
| 0.4.1 | 2026-06-26 | Editable structured fields in cards |
| 0.4.0 | 2026-06-26 | Media workflow modules (Phase 1); desktop datetime runtime |
| 0.3.0 | 2026-06-25 | Split core submodules; WASM JS support; zero Material dependency |
| 0.2.x | earlier | Initial public release; Compose Multiplatform canvas |

---

## Minimum tool versions

| Tool | Minimum version | Notes |
|---|---|---|
| Kotlin | 2.0 | Required for K2 compiler and KMP improvements |
| Gradle | 8.9 | Required by the Gradle plugin ecosystem used |
| Java | 17 | Compile target; 21 recommended for runtime |
| Android Gradle Plugin | 8.7 | Required for `androidLibrary {}` KMP target |
| Compose Multiplatform | 1.7+ | Canvas and editor shell |
| Ktor | 3.x | `graphyn-server` and `io.http_request` node |
| FFmpeg | 4.0+ | `plugins/media-core`; must be on system `PATH` |
| Tesseract | 5.x | `plugins/media-ai` OCR fallback (optional) |

---

## Dependency graph (abbreviated)

```
graphyn-editor
  └── graphyn-editor-api
  └── graphyn-ui-cards
  └── graphyn-plugin-api
        └── graphyn-core-model
              └── graphyn-core-execution
              └── graphyn-core-serialization
              └── graphyn-core-data

graphyn-server
  └── graphyn-plugin-api  (same chain as above)
  └── graphyn-core-{model,execution,serialization,data}
  └── Ktor server (core, netty, sse)
```

Consumers wanting only server-side execution with no editor UI depend on `graphyn-server` alone.
Consumers embedding the editor in a Compose Multiplatform app depend on `graphyn-editor`.
