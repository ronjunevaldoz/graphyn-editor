# app/webApp

Web entry point for the Graphyn demo app. Supports both Wasm and JS targets.

## Running

```bash
# Wasm (recommended — better performance)
./gradlew :app:webApp:wasmJsBrowserDevelopmentRun

# JS
./gradlew :app:webApp:jsBrowserDevelopmentRun
```

## Building for production

```bash
./gradlew :app:webApp:wasmJsBrowserDistribution
./gradlew :app:webApp:jsBrowserDistribution
```

Output lands in `build/dist/`.
