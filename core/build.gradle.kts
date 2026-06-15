import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.JavaExec

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.serialization)
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
       namespace = "com.ronjunevaldoz.graphyn.core"
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
            implementation(libs.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

tasks.register<JavaExec>("benchmarkCore") {
    group = "verification"
    description = "Runs the Graphyn core benchmark snapshot."
    dependsOn("jvmTestClasses")
    mainClass.set("com.ronjunevaldoz.graphyn.core.benchmark.CoreBenchmarkKt")
    classpath = files(
        layout.buildDirectory.dir("classes/kotlin/jvm/test"),
        configurations.getByName("jvmTestRuntimeClasspath"),
    )
}
