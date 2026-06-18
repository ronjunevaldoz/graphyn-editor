# plugins/sample-logger-ui

Editor plugin that adds a custom inspector panel for `sample.logger` nodes.

## What it shows

Displays the node's execution log output in the inspector when a logger node is selected — styled as a dark terminal-like panel with `LOGGER OUTPUT` section label and key/value rows.

## Usage

```kotlin
editorRegistry.install(SampleLoggerEditorPlugin)
```

Depends on `editor-api` and `sample-logger`. No Material dependency.
