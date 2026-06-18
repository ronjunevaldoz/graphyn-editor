# Installation

Add the Maven Central repository if not already present:

```kotlin
repositories {
    mavenCentral()
}
```

Then add the artifacts you need:

```kotlin
// In commonMain.dependencies
api("io.github.ronjunevaldoz:graphyn-core:0.1.0")
api("io.github.ronjunevaldoz:graphyn-plugin-api:0.1.0")
api("io.github.ronjunevaldoz:graphyn-editor-api:0.1.0")
api("io.github.ronjunevaldoz:graphyn-editor:0.1.0")
```

### iOS — Swift Package Manager

Add the binary target to your `Package.swift`:

```swift
.binaryTarget(
    name: "GraphynEditor",
    url: "https://github.com/ronjunevaldoz/graphyn-editor/releases/download/v0.1.0/GraphynEditor.xcframework.zip",
    checksum: "<sha256>"
)
```

Download the checksum from the [GitHub releases page](https://github.com/ronjunevaldoz/graphyn-editor/releases).
