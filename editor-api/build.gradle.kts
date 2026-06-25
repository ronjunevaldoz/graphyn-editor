import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
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
        namespace = "com.ronjunevaldoz.graphyn.editorapi"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // The editor contract exposes model + execution-status types only.
            // Stores/serialization (core:data, core:serialization) are host concerns, not editor ones.
            api(projects.core.model)
            api(projects.core.execution)
            api(libs.kotlinx.coroutinesCore)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

val libraryVersion = (project.findProperty("VERSION") as? String) ?: "0.2.0"

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    if (project.hasProperty("signing.keyId") || project.hasProperty("signingKey")) signAllPublications()
    coordinates("io.github.ronjunevaldoz", "graphyn-editor-api", libraryVersion)
    pom {
        name = "Graphyn Editor API"
        description = "Contract for Graphyn editor panel plugins — register custom inspector UI"
        url = "https://github.com/ronjunevaldoz/graphyn-editor"
        licenses {
            license {
                name = "Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                
            }
        }
        developers {
            developer {
                id = "ronjunevaldoz"
                name = "Ron June Valdoz"
                email = "ronjune.lopez@gmail.com"
            }
        }
        scm {
            url = "https://github.com/ronjunevaldoz/graphyn-editor"
            connection = "scm:git:git://github.com/ronjunevaldoz/graphyn-editor.git"
            developerConnection = "scm:git:ssh://git@github.com/ronjunevaldoz/graphyn-editor.git"
        }
    }
}
