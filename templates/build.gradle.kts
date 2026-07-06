import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Official, parameterized workflow-definition builders (WorkflowDefinition factory functions) for
// graphyn2's own node plugins — the canonical node-graph shape for each operation (sd.* txt2img,
// img2img, ...) lives here once, instead of every consumer (the desktop demo app, Studio's server,
// any other host) hand-rolling the same wiring from scratch. Consumers pass in their own model
// paths/defaults; this module only assembles the graph.
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
        namespace = "com.ronjunevaldoz.graphyn.templates"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

mavenPublishing {
    coordinates(artifactId = "graphyn-templates")
    pom {
        name.set("Graphyn Templates")
        description.set(
            "Official, parameterized WorkflowDefinition builders for graphyn2's first-party node " +
                "plugins (sd.* txt2img/img2img, ...) — the canonical graph shape for each operation, " +
                "reusable by any host instead of being hand-rolled per consumer.",
        )
    }
}
