plugins {
    id("graphyn-kmp-library")
    alias(libs.plugins.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    androidLibrary {
        namespace = "com.ronjunevaldoz.graphyn.core.model"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.serialization.json)
        }
    }
}

val libraryVersion = (project.findProperty("VERSION") as? String) ?: "0.2.1"

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    if (project.hasProperty("signing.keyId") || project.hasProperty("signingInMemoryKey")) signAllPublications()
    coordinates("io.github.ronjunevaldoz", "graphyn-core-model", libraryVersion)
    pom {
        name = "Graphyn Core Model"
        description = "Workflow graph model, types, values, validation, and registry — no Compose, no coroutines."
        url = "https://github.com/ronjunevaldoz/graphyn-editor"
        licenses { license { name = "Apache License, Version 2.0"; url = "https://www.apache.org/licenses/LICENSE-2.0.txt" } }
        developers { developer { id = "ronjunevaldoz"; name = "Ron June Valdoz"; email = "ronjune.lopez@gmail.com" } }
        scm {
            url = "https://github.com/ronjunevaldoz/graphyn-editor"
            connection = "scm:git:git://github.com/ronjunevaldoz/graphyn-editor.git"
            developerConnection = "scm:git:ssh://git@github.com/ronjunevaldoz/graphyn-editor.git"
        }
    }
}
