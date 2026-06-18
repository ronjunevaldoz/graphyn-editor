# app/demo

Demo bootstrap — not published to Maven Central.

Provides `DemoApp` and the bootstrap helpers used by the platform demo apps (desktop, Android, web). Kept separate from `app/shared` so the published library contains no demo or sample plugin dependencies.

## What's here

| Symbol | Purpose |
|---|---|
| `DemoApp` | Pre-wired editor with sample plugins and demo workflow |
| `GraphynBootstrap` | Plugin lists and helper factories for demo use |
| `GraphynDemoWorkflow` | Preloaded `WorkflowDefinition` shown on first launch |
| `GraphynDemoPlugins` | Bundled runtime + editor plugin lists |

## Used by

`app/desktopApp`, `app/androidApp`, `app/webApp` — all call `DemoApp()` directly.

iOS uses `App()` from `app/shared` with the XCFramework instead.
