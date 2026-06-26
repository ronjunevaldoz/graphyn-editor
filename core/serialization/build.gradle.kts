plugins {
    id("graphyn-kmp-library")
    alias(libs.plugins.serialization)
    id("graphyn-maven-publish")
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

mavenPublishing {
    coordinates(artifactId = "graphyn-core-serialization")
    pom {
        name.set("Graphyn Core Serialization")
        description.set("Versioned workflow document codec (JSON) for the Graphyn workflow model.")
    }
}
