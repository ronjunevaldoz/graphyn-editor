plugins {
    id("graphyn-kmp-library")
    alias(libs.plugins.serialization)
    id("graphyn-maven-publish")
}

kotlin {
    androidLibrary {
        namespace = "com.ronjunevaldoz.graphyn.ai"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            implementation(libs.serialization.json)
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
    }
}

mavenPublishing {
    coordinates(artifactId = "graphyn-ai")
    pom {
        name.set("Graphyn AI")
        description.set("LLM workflow generation for Graphyn — Ollama adapter and generator contracts")
    }
}
