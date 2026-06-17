# Preview-Driven Development

Graphyn uses **desktop-first previews** — no Android required.

## How it works

The `org.jetbrains.compose.ui:ui-tooling-preview` artifact (already in `commonMain`) re-exports `androidx.compose.ui.tooling.preview.Preview` on all targets including JVM/Desktop. IntelliJ IDEA 2024.2+ renders these as live desktop Compose windows.

## Rules

- **Preview files live in `jvmMain`**, mirroring the `commonMain` package tree.
- **One file per component**: `GraphynNodeCard.kt` → `GraphynNodeCardPreview.kt` in the same package under `jvmMain`.
- **Always wrap with `GraphynPreview {}`** so previews render with the correct theme.
- **Never put previews in `commonMain`** — preview code compiles into iOS/WASM/JS for no reason.
- **Never put previews in `androidMain`** — defeats the desktop-first goal.

## File layout

```
commonMain/.../canvas/components/GraphynNodeCard.kt
jvmMain/.../canvas/components/GraphynNodeCardPreview.kt
```

## Template

```kotlin
package com.ronjunevaldoz.graphyn.editor.canvas.components

import androidx.compose.runtime.Composable
import com.ronjunevaldoz.graphyn.preview.GraphynPreview
import com.ronjunevaldoz.graphyn.preview.GraphynPreviews

@GraphynPreviews
@Composable
fun MyComponentPreview() {
    GraphynPreview {
        MyComponent(/* sample data */)
    }
}
```

## Imports

```kotlin
// annotation (in jvmMain)
import androidx.compose.ui.tooling.preview.Preview

// convenience wrappers (in jvmMain)
import com.ronjunevaldoz.graphyn.preview.GraphynPreview
import com.ronjunevaldoz.graphyn.preview.GraphynPreviews
```

## Roborazzi connection

The same JVM source set runs Roborazzi screenshot tests. Previews and screenshot tests share the same components and sample data — you can extract preview fixtures into a shared object and reference them from both.

## What NOT to do

- Don't use `@Preview` in `commonMain` — it works but wastes bytes in non-desktop targets.
- Don't import `org.jetbrains.compose.ui.tooling.preview.Preview` — wrong artifact for this project. Use `androidx.compose.ui.tooling.preview.Preview`.
- Don't add complex preview logic. If a preview needs mocking, simplify the component instead.
