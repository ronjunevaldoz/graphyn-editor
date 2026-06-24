# Kotlin Issue: WasmJS IR Deserialization Bug (v2.4.0)

**To file:** Visit https://youtrack.jetbrains.com/issues/KT and create a new issue with the details below.

---

## Title
`WasmJS compiler fails with IR deserialization error on Kotlin 2.4.0`

## Affected Version
Kotlin 2.4.0

## Description

The WasmJS backend fails to compile code that imports complex types (especially those with generic type parameters or type aliases), with an internal IR deserialization error during module loading.

### When It Occurs
- Kotlin 2.3.x: WasmJS compilation succeeds
- Kotlin 2.4.0: WasmJS compilation fails with stack trace below
- Triggered by: Adding imports of types with generics (e.g., `StateFlow<T>`, sealed classes with generics)

### Minimal Reproduction

1. Create a Kotlin Multiplatform project with WasmJS target
2. Add a module with complex types:
   ```kotlin
   // ai/src/commonMain/kotlin/com/example/ai/WorkflowGenerator.kt
   sealed class WorkflowGenerationResult {
       data class Success(val workflow: Map<String, Any>) : WorkflowGenerationResult()
       data class Error(val message: String) : WorkflowGenerationResult()
   }
   
   interface WorkflowGenerator {
       suspend fun generate(prompt: String, catalog: List<Any>): WorkflowGenerationResult
   }
   ```

3. Import this in a parent module (app/shared):
   ```kotlin
   // app/shared/src/commonMain/kotlin/GraphynAiAssistantState.kt
   import com.example.ai.WorkflowGenerator
   import com.example.ai.WorkflowGenerationResult
   
   class GraphynAiAssistantState(private val generator: WorkflowGenerator) { ... }
   ```

4. Import app/shared in webApp module:
   ```gradle
   implementation(projects.app.shared)
   ```

5. Build WasmJS: `./gradlew :webApp:compileProductionExecutableKotlinWasmJs`

### Stack Trace

```
org.jetbrains.kotlin.backend.common.serialization.IrDeclarationDeserializer.deserializeIrTypeAlias(IrDeclarationDeserializer.kt:437)
org.jetbrains.kotlin.backend.common.serialization.IrDeclarationDeserializer.deserializeDeclaration(IrDeclarationDeserializer.kt:843)
org.jetbrains.kotlin.backend.common.serialization.IrDeclarationDeserializer.deserializeDeclaration$default(IrDeclarationDeserializer.kt:830)
org.jetbrains.kotlin.backend.common.serialization.IrFileDeserializer.deserializeDeclaration(IrFileDeserializer.kt:45)
org.jetbrains.kotlin.backend.common.serialization.FileDeserializationState.deserializeAllFileReachableTopLevel(IrFileDeserializer.kt:179)
org.jetbrains.kotlin.backend.common.serialization.BasicIrModuleDeserializer$ModuleDeserializationState.deserializeReachableDeclarations(BasicIrModuleDeserializer.kt:215)
org.jetbrains.kotlin.ir.backend.js.KlibKt.loadIr(klib.kt:134)
org.jetbrains.kotlin.cli.pipeline.web.wasm.WholeWorldCompilerBase.loadIr(KotlinIr2WasmIrCompiler.kt:51)
org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmBackendPipelinePhase.compileNonIncrementally(WasmBackendPipelinePhase.kt:82)

Execution failed for task ':app:webApp:compileProductionExecutableKotlinWasmJs'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleKotlinCompilerWorkAction
   > Internal compiler error. See log for more details
```

### Additional Context

- **Project:** https://github.com/ronjunevaldoz/graphyn-editor (open source, KMP workflow editor)
- **Commit that triggered the issue:** b578f3e (added AI assistant module with complex sealed types)
- **Workaround:** Disable WasmJS builds in CI; JVM/native targets compile fine
- **Expected behavior:** WasmJS should deserialize IR and compile successfully (as in 2.3.x)

### Environment
- Kotlin: 2.4.0
- Compose Multiplatform: 1.11.1
- Gradle: 9.1.0
- OS: macOS

---

## Comments for Kotlin Team
The issue appears to be in the WasmJS IR deserialization step when the compiler encounters certain type alias or generic type patterns. Since 2.3.x handles the same code without issue, this is likely a regression in 2.4.0's IR serialization format or deserialization logic.
