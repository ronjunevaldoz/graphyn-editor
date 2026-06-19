# Embedding on Web

Graphyn targets both **Kotlin/JS** (webpack-based) and **Kotlin/WASM** (wasmJs). WASM is the recommended target for production — it is faster at runtime, but requires a browser that supports WebAssembly GC (Chrome 119+, Firefox 120+, Safari 17.4+).

---

## Which target to use

| | JS (`js { browser() }`) | WASM (`wasmJs { browser() }`) |
|---|---|---|
| Browser support | All modern browsers | Chrome 119+, Firefox 120+, Safari 17.4+ |
| Startup time | Faster | Slightly slower (WASM compilation) |
| Runtime speed | Good | Better |
| Bundle size | Larger | Smaller |
| Recommended | Fallback / broad compat | Production default |

---

## Adding the dependency

```kotlin
// shared/build.gradle.kts
kotlin {
    js { browser() }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            implementation("io.github.ronjunevaldoz:graphyn-editor:0.1.0")
        }
    }
}
```

---

## Entry point

```kotlin
// webApp/src/wasmJsMain/kotlin/Main.kt

fun main() {
    CanvasBasedWindow(title = "My Workflow Editor") {
        DemoApp()  // or your own GraphynEditorShell wrapper
    }
}
```

For Kotlin/JS the entry point is the same — `CanvasBasedWindow` works on both targets.

---

## Content Security Policy

If your deployment has a CSP header, you need to allow WebAssembly evaluation for the WASM target:

```
Content-Security-Policy: default-src 'self'; script-src 'self' 'wasm-unsafe-eval';
```

The JS target requires `'unsafe-eval'` instead (less restrictive — prefer WASM for CSP-sensitive deployments).

---

## Outbound HTTP from the browser

The `io.http_request` executor runs in the browser context. Requests are subject to CORS — the target server must return appropriate `Access-Control-Allow-Origin` headers. For production, proxy outbound requests through your own backend to avoid CORS issues and to keep credentials server-side. See [Remote Execution](remote-execution.md).

---

## Offline support

Graphyn itself has no offline/service-worker integration. If you need the editor to work offline, ensure your bundled assets are cached by a service worker and that your persistence layer (see [Persistence](persistence.md)) handles the case where the backend is unreachable:

```kotlin
LaunchedEffect(state.workflow) {
    val wf = state.workflow ?: return@LaunchedEffect
    delay(1_000)
    try {
        api.save(wf)
    } catch (e: NetworkException) {
        localStorage.setItem("draft-${wf.id}", wf.toJson())
    }
}
```

---

## Keyboard shortcut conflicts

Browsers intercept some key combinations (e.g., `Cmd+S` for save, `Cmd+W` to close tab). Add `event.preventDefault()` in your host page if you need to intercept these before the browser sees them. The Compose event loop handles this inside the canvas — shortcuts that land in the canvas work without extra configuration.
