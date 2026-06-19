# Plugin Versioning

Graphyn validates plugin compatibility at install time using `apiVersion`. This page explains the contract and what to do when it changes.

---

## How version checking works

Every plugin declares an `apiVersion` in its metadata:

```kotlin
override val metadata = GraphynPluginMetadata(
    id = "myplugin",
    displayName = "My Plugin",
    version = "1.0.0",
    apiVersion = GRAPHYN_PLUGIN_API_VERSION,  // always use the constant
)
```

`DefaultGraphynPluginRegistry.installAll` will throw if `plugin.metadata.apiVersion != GRAPHYN_PLUGIN_API_VERSION`. This prevents a plugin compiled against an old API from silently misbehaving at runtime.

---

## Current API version

```kotlin
const val GRAPHYN_PLUGIN_API_VERSION = 1
```

The same constant exists in `editor-api`:

```kotlin
const val GRAPHYN_EDITOR_PLUGIN_API_VERSION = 1
```

Always import the constant — never hardcode the integer — so your plugin automatically picks up the bump when you upgrade Graphyn.

---

## When the API version changes

`GRAPHYN_PLUGIN_API_VERSION` is incremented when the `GraphynPluginRegistrar` or `GraphynPlugin` interface changes in a way that is not binary-compatible. Typical causes:

- A new required method added to `GraphynPlugin`
- A parameter added to `registerNodeSpec` or `registerExecutor`
- A breaking change to `NodeSpec` or `PortSpec` fields

The Graphyn changelog will document what changed and what you need to update in your plugin.

---

## Deprecation path

When a registrar method is deprecated, the process is:

1. The old method is annotated `@Deprecated` with a `ReplaceWith` suggestion.
2. It stays functional for one minor version.
3. It is removed in the following minor version.

If your plugin calls a deprecated method, the compiler will emit a warning. Treat warnings as errors in your plugin's CI build to catch these early:

```kotlin
// build.gradle.kts
kotlin {
    compilerOptions {
        allWarningsAsErrors = true
    }
}
```

---

## Spec versioning vs API versioning

`apiVersion` is the *library API* version — it governs registrar compatibility. It is separate from:

- **Your plugin's `version`** — a semver string you control, shown in the palette and debugging tools.
- **The workflow JSON `schemaVersion`** — governs serialized data compatibility.

These three version numbers are independent. See [Serialization](../reference/serialization.md) for the schema version story.

---

## Third-party plugin compatibility matrix

If you publish a plugin for others to use, document which Graphyn versions it supports:

```
my-plugin 1.0.x → graphyn-plugin-api 0.1.x (apiVersion 1)
my-plugin 2.0.x → graphyn-plugin-api 0.2.x (apiVersion 2, if/when released)
```

Publish a `COMPATIBILITY.md` alongside your plugin's README.
