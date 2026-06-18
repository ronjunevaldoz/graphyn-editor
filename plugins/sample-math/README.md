# plugins/sample-math

Reference runtime plugin demonstrating numeric node types.

## Nodes

| Type | Inputs | Output |
|---|---|---|
| `math.add` | `left: Double`, `right: Double` | `result: Double` |
| `math.subtract` | `left: Double`, `right: Double` | `result: Double` |
| `math.multiply` | `left: Double`, `right: Double` | `result: Double` |

## Usage

```kotlin
registry.install(MathPlugin)
```

All ports use `WorkflowType.DoubleType`. Default values are `0.0` for all inputs. Good reference for building numeric pipeline nodes.
