# Plugin Authoring

A Graphyn plugin has two parts:

- **Runtime plugin** (`GraphynPlugin`) — defines node specs and executors. No Compose dependency.
- **Editor plugin** (`GraphynEditorPlugin`) — registers custom card renderers and palette categories. Depends on Compose.

Both are optional. A plugin with only a runtime part will render using the default `FieldCard`. A plugin with only an editor part has no executable behaviour.

---

## 1. Create the module

In `settings.gradle.kts`:

```kotlin
include(":plugins:my-plugin")
```

`plugins/my-plugin/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    iosArm64(); iosSimulatorArm64(); jvm()
    js { browser() }
    wasmJs { browser() }
    androidLibrary {
        namespace = "com.example.graphyn.plugins.myplugin"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.pluginApi)
            api(projects.editorApi)   // only if you have an editor plugin
            api(projects.ui.cards)    // only if you use FieldCardFactory
        }
    }
}
```

---

## 2. Define node specs

A `NodeSpec` describes what a node does, what ports it has, and what default values it ships with.

```kotlin
// plugins/my-plugin/src/commonMain/.../MyPluginSpecs.kt

internal val specSummarize = NodeSpec(
    type = "myplugin.summarize",
    label = "Summarize",
    description = "Produces a short summary of a longer text.",
    category = "myplugin.text",
    inputs = listOf(
        PortSpec("text",     WorkflowType.StringType,  description = "Full text to summarize"),
        PortSpec("maxWords", WorkflowType.IntType,     description = "Maximum word count for the summary"),
    ),
    outputs = listOf(
        PortSpec("summary", WorkflowType.StringType, description = "Shortened text"),
    ),
    defaultValues = mapOf(
        "maxWords" to WorkflowValue.IntValue(50),
    ),
)
```

**Port naming rules:**
- Port IDs are plain names (`"text"`, `"maxWords"`) — not `"name:type"` strings.
- Match `PortSpec` IDs exactly when calling `registerExecutor`.

---

## 3. Write the runtime plugin

```kotlin
// plugins/my-plugin/src/commonMain/.../MyPlugin.kt

object MyPlugin : GraphynPlugin {

    override val metadata = GraphynPluginMetadata(
        id = "myplugin",
        displayName = "My Plugin",
        version = "1.0.0",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        registrar.registerNodeSpec(specSummarize)

        registrar.registerExecutor(specSummarize.type) { inputs ->
            val text     = (inputs["text"]     as? WorkflowValue.StringValue)?.value ?: ""
            val maxWords = (inputs["maxWords"] as? WorkflowValue.IntValue)?.value    ?: 50
            val summary  = text.split(" ").take(maxWords).joinToString(" ")
            mapOf("summary" to WorkflowValue.StringValue(summary))
        }
    }
}
```

The executor receives the resolved input values (after upstream nodes have run) and returns a map of output values. Output keys must match `PortSpec` IDs in the spec.

---

## 4. Write the editor plugin

Register a card renderer and a palette category:

```kotlin
// plugins/my-plugin/src/commonMain/.../MyEditorPlugin.kt

object MyEditorPlugin : GraphynEditorPlugin {

    override val metadata = GraphynEditorPluginMetadata(
        id = "myplugin.editor",
        displayName = "My Plugin Editor",
        version = "1.0.0",
        apiVersion = GRAPHYN_EDITOR_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynEditorPluginRegistrar) {
        // Use the built-in FieldCard with 2 input rows and 1 output row
        registrar.registerCanvasCard(
            specSummarize.type,
            FieldCardFactory(inputRows = 2, outputRows = 1),
        )

        // Register a palette category with a colour (ARGB long)
        registrar.registerCategory(
            "myplugin.text",
            NodeCategoryMeta("Text AI", 0xFF8B5CF6L),
        )
    }
}
```

For a fully custom card, implement `NodeCanvasFactory` directly. See [Custom Cards](custom-cards.md).

---

## 5. Register in the app

In your bootstrap / `GraphynApp` equivalent:

```kotlin
val runtimePlugins = listOf(MyPlugin, /* other plugins */)
val editorPlugins  = listOf(MyEditorPlugin, /* other plugins */)

val pluginRegistry = DefaultGraphynPluginRegistry().apply { installAll(runtimePlugins) }
val editorRegistry = DefaultGraphynEditorPluginRegistry().apply { installAll(editorPlugins) }
```

Or if you're using `GraphynBootstrap` from `app/demo`, add them to `GraphynDemoPlugins`.

---

## 6. Test the plugin

```kotlin
// plugins/my-plugin/src/commonTest/.../MyPluginTest.kt

class MyPluginTest {
    @Test
    fun summarize_truncates_to_max_words() {
        val registrar = TestPluginRegistrar()
        MyPlugin.register(registrar)

        val executor = registrar.executorFor("myplugin.summarize")
        val output = runTest {
            executor(mapOf(
                "text"     to WorkflowValue.StringValue("one two three four five six"),
                "maxWords" to WorkflowValue.IntValue(3),
            ))
        }

        assertEquals("one two three", (output["summary"] as WorkflowValue.StringValue).value)
    }
}
```

---

## Do's and Don'ts

| Do | Don't |
|---|---|
| Model a real, named operation in each spec | Add placeholder or demo-only specs to a plugin |
| Put demo workflows in `app/demo` | Define sample `WorkflowDefinition` data inside a plugin |
| Keep `plugin-api` and `editor-api` separate | Import Compose in a runtime-only plugin |
| Use `OpaqueType` for untyped pass-through ports | Use `OpaqueType` as a shortcut to avoid thinking about types |
