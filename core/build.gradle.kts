import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.JavaExec

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.serialization)
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

mavenPublishing {
    coordinates("io.github.ronjunevaldoz", "graphyn-core", "0.1.0")
    pom {
        name = "Graphyn Core"
        description = "Workflow model, types, validation, and execution engine — no Compose dependency"
        url = "https://github.com/ronjunevaldoz/graphyn-editor"
        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/licenses/MIT"
                
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
