import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.serialization)
}

kotlin {
    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    androidLibrary {
        namespace = "com.ronjunevaldoz.graphyn.app"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.app.shared)
            api(projects.runtime)
            implementation(projects.templates)
            implementation(projects.plugins.sampleLogger)
            implementation(projects.plugins.sampleLoggerUi)
            implementation(projects.plugins.sampleStyleNodes)
            implementation(projects.plugins.stickyNotes)
            implementation(projects.plugins.listOps)
            implementation(projects.plugins.control)
            implementation(projects.plugins.types)
            implementation(projects.plugins.text)
            implementation(projects.plugins.io)
            implementation(projects.plugins.json)
            implementation(projects.plugins.preview)
            implementation(projects.plugins.shorts)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
        }
        jvmMain.dependencies {
            implementation(projects.plugins.gmail)
            implementation(projects.plugins.linkedin)
            implementation(projects.plugins.script)
            implementation(projects.plugins.stableDiffusion)
            implementation(projects.plugins.mediaCore)
            implementation(projects.plugins.mediaAi)
            implementation(projects.core.serialization)
            implementation(libs.ktor.clientCore)
            implementation(libs.ktor.clientCio)
            implementation(libs.serialization.json)
        }
        jsMain.dependencies {
            implementation(projects.plugins.gmail)
            implementation(projects.plugins.linkedin)
        }
        androidMain.dependencies {
            implementation(projects.plugins.gmail)
            implementation(projects.plugins.linkedin)
        }
        wasmJsMain.dependencies {
            implementation(projects.plugins.gmail)
            implementation(projects.plugins.linkedin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(compose.desktop.uiTestJUnit4)
            implementation(libs.kotlinx.coroutinesTest)
            implementation(libs.roborazzi.compose.desktop)
            implementation(projects.plugins.script)
        }
    }
}

// No `application` plugin here — this is a KMP library module. This task runs WorkflowCliRunner.kt's
// main() against the jvm target's own compiled output + runtime classpath, addressed by the standard
// KMP-generated task/configuration names (stable across Kotlin Gradle Plugin versions) instead of the
// internal KotlinCompilation API, whose accessor names shift between KGP releases.
tasks.register<JavaExec>("runWorkflowCli") {
    group = "application"
    description = "Run a workflow headlessly: ./gradlew :app:demo:runWorkflowCli --args=\"workflow=storyboard topic='a cozy coffee shop'\""
    dependsOn("jvmMainClasses")
    classpath = files(layout.buildDirectory.dir("classes/kotlin/jvm/main")) +
        files(layout.buildDirectory.dir("processedResources/jvm/main")) +
        configurations.getByName("jvmRuntimeClasspath")
    mainClass.set("com.ronjunevaldoz.graphyn.bootstrap.WorkflowCliRunnerKt")
    // Defaults to the real dev Ollama deployment — without these, GRAPHYN_OLLAMA_HOST/MODEL are
    // unset in the Gradle daemon's environment, silently falling back to http://localhost:11434 +
    // llama3.1, which isn't installed there; every storyboard generation then failed to parse the
    // resulting {"error": "model 'llama3.1' not found"} response and fell back to a hardcoded
    // placeholder storyboard on every run. Still overridable via real environment variables.
    environment("GRAPHYN_OLLAMA_HOST", System.getenv("GRAPHYN_OLLAMA_HOST") ?: "https://ron-local-home.duckdns.org/ollama")
    environment("GRAPHYN_OLLAMA_MODEL", System.getenv("GRAPHYN_OLLAMA_MODEL") ?: "qwen3:8b")
}
