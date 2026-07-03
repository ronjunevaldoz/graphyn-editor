# Compatibility Matrix

## Published artifacts

All artifacts share the same version and are published to [Maven Central](https://central.sonatype.com/search?q=io.github.ronjunevaldoz) under `io.github.ronjunevaldoz`.

| Artifact | Maven coordinate | Since | Description |
|---|---|---|---|
| `graphyn-core-model` | `io.github.ronjunevaldoz:graphyn-core-model` | 0.3.0 | Workflow graph types, type system, registry, validation |
| `graphyn-core-execution` | `io.github.ronjunevaldoz:graphyn-core-execution` | 0.3.0 | Execution engine and events |
| `graphyn-core-serialization` | `io.github.ronjunevaldoz:graphyn-core-serialization` | 0.3.0 | Workflow JSON codec |
| `graphyn-core-data` | `io.github.ronjunevaldoz:graphyn-core-data` | 0.3.0 | Workflow stores + platform persistence |
| `graphyn-ui-design` | `io.github.ronjunevaldoz:graphyn-ui-design` | **0.7.1** | Compose Multiplatform design tokens, theme, and UI primitives |
| `graphyn-plugin-api` | `io.github.ronjunevaldoz:graphyn-plugin-api` | 0.3.0 | Contracts for runtime node plugins |
| `graphyn-editor-api` | `io.github.ronjunevaldoz:graphyn-editor-api` | 0.3.0 | Contracts for editor canvas cards and panels |
| `graphyn-ui-cards` | `io.github.ronjunevaldoz:graphyn-ui-cards` | 0.3.0 | Built-in canvas card renderers (FieldCard, ShapeCard, CircleCard) |
| `graphyn-editor` | `io.github.ronjunevaldoz:graphyn-editor` | 0.3.0 | Compose Multiplatform editor shell, canvas, launcher |
| `graphyn-ai` | `io.github.ronjunevaldoz:graphyn-ai` | **0.7.0** | LLM workflow generation — Ollama adapter and generator contracts |
| `graphyn-runtime` | `io.github.ronjunevaldoz:graphyn-runtime` | **0.7.0** | Convenience bundle of all first-party plugins (control, list-ops, types, text, io, json, preview) |
| `graphyn-ktor-plugin` | `io.github.ronjunevaldoz:graphyn-ktor-plugin` | **0.6.0** (renamed next release) | Ktor server + `install(Graphyn)` embeddable plugin |

> **`graphyn-ktor-plugin` renamed from `graphyn-server`.** Versions 0.6.0–0.7.6 remain published and frozen on Maven Central as `io.github.ronjunevaldoz:graphyn-server` — that coordinate receives no further updates. Starting with the next release this artifact publishes as `io.github.ronjunevaldoz:graphyn-ktor-plugin`; existing consumers must switch coordinates manually to keep receiving updates.
> **`graphyn-runtime` first published in 0.7.0** — was previously missing publish config (library bug fixed in 0.7.0).
> **`graphyn-ui-design` first published in 0.7.1** — was previously an internal-only module; promoted to fix unpublished coordinate leaking into `graphyn-editor` POM.

---

## Kotlin Multiplatform targets

| Artifact | JVM | Android | JS (browser) | WASM JS | iOS |
|---|:---:|:---:|:---:|:---:|:---:|
| `graphyn-core-model` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `graphyn-core-execution` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `graphyn-core-serialization` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `graphyn-core-data` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `graphyn-ui-design` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `graphyn-plugin-api` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `graphyn-editor-api` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `graphyn-ui-cards` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `graphyn-ai` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `graphyn-runtime` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `graphyn-editor` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `graphyn-ktor-plugin` | ✅ | — | — | — | — |

`graphyn-ktor-plugin` is JVM-only — it is a Ktor server, not a KMP library.

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
| 0.7.4 | 2026-06-27 | Publish all 13 first-party plugins as individual artifacts; `graphyn-ui-design` promoted from internal; reverse audit guard in `verifyPublishing` |
| 0.7.2 | 2026-06-27 | First publish of 7 bundled plugins (`control`, `list-ops`, `types`, `text`, `io`, `json`, `preview`) fixing `graphyn-runtime` POM |
| 0.7.1 | 2026-06-27 | Fix `graphyn-editor` POM (removed `sample-logger*` and `core:designsystem` leaks) |
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
| Ktor | 3.x | `graphyn-ktor-plugin` and `io.http_request` node |
| FFmpeg | 4.0+ | `plugins/media-core`; must be on system `PATH` |
| Tesseract | 5.x | `plugins/media-ai` OCR fallback (optional) |

---

## Dependency graph (abbreviated)

```
graphyn-editor
  └── graphyn-editor-api
  └── graphyn-ui-cards
  └── graphyn-ui-design
  └── graphyn-plugin-api
        └── graphyn-core-model
              └── graphyn-core-execution
              └── graphyn-core-serialization
              └── graphyn-core-data

graphyn-runtime
  └── graphyn-plugin-api  (same chain as above)
  └── graphyn-editor-api

graphyn-ktor-plugin
  └── graphyn-runtime
  └── graphyn-core-{model,execution,serialization,data}
  └── Ktor server (core, netty, sse)
```

Consumers wanting only server-side execution with no editor UI depend on `graphyn-ktor-plugin` alone.
Consumers embedding the editor in a Compose Multiplatform app depend on `graphyn-editor`.
