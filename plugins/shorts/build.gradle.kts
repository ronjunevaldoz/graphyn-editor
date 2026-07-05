import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Storyboard-first vertical-shorts pipeline: composes existing sd.* / media.* / io.* / json.* nodes
// (all referenced by type string, so this module doesn't depend on those plugins) plus its own
// Ollama storyboard call + JSON validation/fallback executors and the Flux-txt2img + Ken Burns
// per-scene subgraph builder. Multiplatform so the shared workflow catalog can build these
// definitions on every target; only unloadOllamaModel() has a JVM/Android actual (js/wasm/ios are
// no-ops — those targets never drive local Ollama generation).
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    id("graphyn-maven-publish")
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    androidLibrary {
        namespace = "com.ronjunevaldoz.graphyn.plugins.shorts"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.pluginApi)
            implementation(libs.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
        }
    }
}

mavenPublishing {
    coordinates(artifactId = "graphyn-plugin-shorts")
    pom {
        name.set("Graphyn Plugin: shorts")
        description.set(
            "First-party Graphyn plugin for storyboard-first vertical shorts — an Ollama " +
                "storyboard generator (with JSON validation and a known-good fallback) plus the " +
                "reusable Flux txt2img + Ken Burns per-scene subgraph and caption/stitch " +
                "composition builders, layered over the sd.* and media.* node families.",
        )
    }
}
