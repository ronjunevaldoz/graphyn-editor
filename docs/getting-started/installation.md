# Installation

Add the Maven Central repository if not already present:

```kotlin
repositories {
    mavenCentral()
}
```

Then add the artifacts you need:

```kotlin
// Required — workflow model, execution engine, serialization
api("io.github.ronjunevaldoz:graphyn-core:0.2.0")

// Required — plugin and editor plugin contracts
api("io.github.ronjunevaldoz:graphyn-plugin-api:0.2.0")
api("io.github.ronjunevaldoz:graphyn-editor-api:0.2.0")

// Required — Compose Multiplatform editor shell (canvas, palette, inspector)
api("io.github.ronjunevaldoz:graphyn-editor:0.2.0")

// Optional — pre-built card styles (FieldCard, CircleCard, ShapeCard)
implementation("io.github.ronjunevaldoz:graphyn-ui-cards:0.2.0")

// Optional — HTTP Request, File Read, File Write executor nodes
implementation("io.github.ronjunevaldoz:graphyn-plugin-io:0.2.0")
```

### iOS — Swift Package Manager

Add the binary target to your `Package.swift`:

```swift
.binaryTarget(
    name: "GraphynEditor",
    url: "https://github.com/ronjunevaldoz/graphyn-editor/releases/download/v0.2.0/GraphynEditor.xcframework.zip",
    checksum: "<sha256>"
)
```

Download the checksum from the [GitHub releases page](https://github.com/ronjunevaldoz/graphyn-editor/releases).
