plugins {
    id("graphyn-kmp-library")
    alias(libs.plugins.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
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

val libraryVersion = (project.findProperty("VERSION") as? String) ?: "0.6.0"

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    if (project.hasProperty("signing.keyId") || project.hasProperty("signingKey")) signAllPublications()
    coordinates("io.github.ronjunevaldoz", "graphyn-ai", libraryVersion)
    pom {
        name = "Graphyn AI"
        description = "LLM workflow generation for Graphyn — Ollama adapter and generator contracts"
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
