# Embedding on Android

Add the dependencies to your Android app module:

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("io.github.ronjunevaldoz:graphyn-editor:0.1.0")
    implementation("io.github.ronjunevaldoz:graphyn-plugin-api:0.1.0")
}
```

---

## Activity / Fragment

Graphyn is a Compose Multiplatform library. Embed it inside a standard `ComponentActivity` with `setContent`:

```kotlin
class WorkflowEditorActivity : ComponentActivity() {

    private val workflowId by lazy { intent.getStringExtra("workflowId") ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WorkflowEditorScreen(workflowId = workflowId)
        }
    }
}

@Composable
fun WorkflowEditorScreen(workflowId: String) {
    val repo = remember { WorkflowRepository() }
    val workflow = produceState<WorkflowDefinition?>(null, workflowId) {
        value = repo.load(workflowId)
    }.value

    key(workflowId) {
        val state = rememberGraphynEditorState(initialWorkflow = workflow)

        // Auto-save
        LaunchedEffect(state.workflow) {
            val wf = state.workflow ?: return@LaunchedEffect
            delay(1_000)
            repo.save(wf)
        }

        GraphynEditorShell(
            branding = GraphynBranding(appName = "My App"),
            dependencies = GraphynEditorShellDependencies(
                nodeSpecs = repo.nodeSpecs,
                executionEngine = WorkflowExecutionEngine(repo.executors, repo.nodeSpecs),
            ),
            state = state,
        )
    }
}
```

---

## Fragment interop

If you're using Fragments, host the composable with `ComposeView`:

```kotlin
class WorkflowFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext()).apply {
            setContent {
                WorkflowEditorScreen(workflowId = requireArguments().getString("workflowId")!!)
            }
        }
}
```

---

## Back handling

The editor does not intercept the system back button. If you want `Back` to prompt the user to save unsaved changes, wrap the shell with `BackHandler`:

```kotlin
val isDirty = state.workflow != initialWorkflow
BackHandler(enabled = isDirty) {
    showSaveDialog = true
}
```

---

## Window insets

On Android, system bars (status bar, navigation bar) may overlap the editor. Apply window insets before the editor fills the screen:

```kotlin
Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
    GraphynEditorShell(...)
}
```

---

## Canvas bounds

Use `GraphynCanvasBounds` to restrict how far the user can pan — useful when the editor is embedded in a smaller portion of the screen:

```kotlin
val state = rememberGraphynEditorState(
    initialWorkflow = workflow,
    canvasBounds = GraphynCanvasBounds(
        widthDp = 800,
        heightDp = 600,
    ),
)
```

---

## Permissions

The `io.http_request` plugin executor makes outbound network calls. Add the Internet permission to your `AndroidManifest.xml` if you use it:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```
