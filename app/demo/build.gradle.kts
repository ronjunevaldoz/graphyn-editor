import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.roborazzi)
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
            api(projects.runtime)
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
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
        }
        jvmMain.dependencies {
            implementation(projects.plugins.gmail)
            implementation(projects.plugins.linkedin)
            implementation(projects.plugins.mediaCore)
            implementation(projects.plugins.mediaAi)
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
            implementation(libs.roborazzi.compose.desktop)
            implementation(projects.plugins.script)
        }
    }
}
