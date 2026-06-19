import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    val xcf = XCFramework("GraphynEditor")
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "GraphynEditor"
            isStatic = true
            xcf.add(this)
        }
    }
    
    jvm()
    
    js {
        browser()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    androidLibrary {
       namespace = "com.ronjunevaldoz.graphyn.app.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
   
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
        }
        commonMain.dependencies {
            api(projects.core)
            api(projects.core.designsystem)
            api(projects.editorApi)
            api(projects.pluginApi)
            implementation(projects.plugins.sampleLogger)
            implementation(projects.plugins.sampleLoggerUi)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.compose.uiTest)
            implementation(libs.kotlinx.coroutinesTest)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(compose.desktop.uiTestJUnit4)
            implementation(libs.roborazzi.compose.desktop)
            implementation(projects.plugins.sampleStyleNodes)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
        wasmJsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

val libraryVersion = (project.findProperty("VERSION") as? String) ?: "0.1.0"

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    if (project.hasProperty("signing.keyId") || project.hasProperty("signingKey")) signAllPublications()
    coordinates("io.github.ronjunevaldoz", "graphyn-editor", libraryVersion)
    pom {
        name = "Graphyn Editor"
        description = "Compose Multiplatform workflow editor canvas — canvas, palette, inspector, and gesture handling."
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
