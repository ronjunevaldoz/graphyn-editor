# app/app

The Graphyn application module — not published to Maven Central.

Provides `GraphynApp` and the bootstrap helpers that wire the editor together for every platform.
Kept separate from `app/shared` (the reusable editor library) so the published library carries no
application or sample-plugin dependencies.

## What's here

| Symbol | Purpose |
|---|---|
| `GraphynApp` | Pre-wired editor + launcher with the bundled plugins and template catalog |
| `WorkflowCatalog` | Bundled templates, each tagged with a launcher `WorkflowCategory` + description |
| `GraphynBootstrap` | Plugin lists and helper factories for app wiring |
| `GraphynDemoPlugins` | Bundled runtime + editor plugin lists |

## Used by

`app/desktopApp`, `app/androidApp`, `app/webApp` — all call `GraphynApp()` directly.

iOS uses `App()` from `app/shared` with the XCFramework instead.
