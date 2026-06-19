# Running locally

The repo contains three runnable apps. Each has a different purpose.

---

## `app/desktopApp` — the production editor

The full, production-grade workflow editor. This is the app you build and ship to end users.

```bash
./gradlew :app:desktopApp:run
```

**What it includes:**
- All first-party plugins (math, IO, style-nodes, sticky notes, list ops, control, types, text)
- Workflow launcher home screen (templates + recents)
- Subgraph drill-in navigation
- Full theme / appearance controls

This app is backed by `app/demo` which owns plugin registration and demo workflow definitions.
Production features (file open/save, recent files, unsaved-changes confirmation) are being
added to `app/desktopApp` directly — not to `app/demo`.

---

## `app/sample` — the consumer integration example

A minimal JVM Desktop app that shows how to embed Graphyn as a library in your own project.
Intended as a living reference for external consumers.

```bash
./gradlew :app:sample:run
```

**What it shows:**
- Installing a single plugin (`MathPlugin`) via `DefaultGraphynPluginRegistry`
- Passing specs and executors to `WorkflowExecutionEngine` and `GraphynEditorShell`
- Auto-saving the workflow to `~/.graphyn/sample-workflow.json` using `workflowFlow`

Source: [`app/sample/src/main/kotlin/com/ronjunevaldoz/graphyn/sample/SampleApp.kt`](../../app/sample/src/main/kotlin/com/ronjunevaldoz/graphyn/sample/SampleApp.kt)

---

## `app/demo` — plugin showcase (not a runnable target)

A KMP library module, not a standalone app. It:
- Registers all first-party plugins into a `GraphynBootstrap` helper
- Defines demo `WorkflowDefinition` scenes used by `app/desktopApp`
- Hosts `DemoApp` — the top-level composable `app/desktopApp` calls

Do not add persistence, file management, or production features here. Those belong in
`app/desktopApp`. Do not add registered node specs for demo purposes only — use local
`WorkflowDefinition` data inside `DemoScenes` instead.

---

## Other targets

| Target | Command |
|---|---|
| Android | `./gradlew :app:androidApp:installDebug` |
| Web (JS) | `./gradlew :app:webApp:jsBrowserRun` |
| Web (WASM) | `./gradlew :app:webApp:wasmJsBrowserRun` |
| Tests | `./gradlew :app:shared:jvmTest` |
