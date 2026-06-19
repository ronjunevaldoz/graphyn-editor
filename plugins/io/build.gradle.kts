import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()
    jvm()
    js { browser() }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }

    androidLibrary {
        namespace = "com.ronjunevaldoz.graphyn.plugins.io"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.pluginApi)
            api(projects.editorApi)
            api(projects.ui.cards)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.ktor.clientCore)
        }
        val jvmMain by getting {
            dependencies { implementation(libs.ktor.clientCio) }
        }
        val androidMain by getting {
            dependencies { implementation(libs.ktor.clientOkhttp) }
        }
        val iosMain by creating {
            dependsOn(commonMain.get())
            dependencies { implementation(libs.ktor.clientDarwin) }
        }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
        val jsMain by getting {
            dependencies { implementation(libs.ktor.clientJs) }
        }
        val wasmJsMain by getting {
            dependencies { implementation(libs.ktor.clientJs) }
        }
        commonTest.dependencies { implementation(libs.kotlin.test) }
    }
}
