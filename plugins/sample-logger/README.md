# plugins/sample-logger

Reference runtime plugin — a single `sample.logger` node that passes a string message through.

## Node spec

| | Port | Type |
|---|---|---|
| Input | `message` | `StringType` |
| Output | `message` | `StringType` |

## Usage

```kotlin
registry.install(SampleLoggerPlugin)
```

The executor echoes the input `message` to output unchanged. Used in the default demo workflow to demonstrate connected node execution.
