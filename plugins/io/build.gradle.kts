plugins {
    id("graphyn-kmp-compose-library")
    id("graphyn-maven-publish")
}

kotlin {
    androidLibrary {
        namespace = "com.ronjunevaldoz.graphyn.plugins.io"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.pluginApi)
            api(projects.editorApi)
            api(projects.ui.cards)
            implementation(projects.core.common)
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
    }
}

mavenPublishing {
    coordinates(artifactId = "graphyn-plugin-io")
    pom {
        name.set("Graphyn Plugin: io")
        description.set("First-party Graphyn plugin.")
    }
}
