import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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
        namespace = "com.ronjunevaldoz.graphyn.demo"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.app.shared)
            implementation(projects.plugins.sampleLogger)
            implementation(projects.plugins.sampleLoggerUi)
            implementation(projects.plugins.sampleStyleNodes)
            implementation(projects.plugins.stickyNotes)
            implementation(projects.plugins.listOps)
            implementation(projects.plugins.control)
            implementation(projects.plugins.types)
            implementation(projects.plugins.text)
            implementation(projects.plugins.io)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(compose.desktop.uiTestJUnit4)
        }
    }
}
