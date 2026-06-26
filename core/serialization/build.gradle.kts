plugins {
    id("graphyn-kmp-library")
    alias(libs.plugins.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    androidLibrary {
        namespace = "com.ronjunevaldoz.graphyn.core.serialization"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            implementation(libs.serialization.json)
        }
    }
}

val libraryVersion = (project.findProperty("VERSION") as? String) ?: "0.2.1"

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    if (project.hasProperty("signing.keyId") || project.hasProperty("signingKey")) signAllPublications()
    coordinates("io.github.ronjunevaldoz", "graphyn-core-serialization", libraryVersion)
    pom {
        name = "Graphyn Core Serialization"
        description = "Versioned workflow document codec (JSON) for the Graphyn workflow model."
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
