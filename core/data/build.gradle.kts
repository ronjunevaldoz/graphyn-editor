plugins {
    id("graphyn-kmp-library")
    alias(libs.plugins.serialization)
    id("graphyn-maven-publish")
}

kotlin {
    androidLibrary {
        namespace = "com.ronjunevaldoz.graphyn.core.data"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            implementation(libs.serialization.json)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.datetime)
        }
    }
}

mavenPublishing {
    coordinates(artifactId = "graphyn-core-data")
    pom {
        name.set("Graphyn Core Data")
        description.set("Workflow stores, diffing, versioning, and platform persistence for the Graphyn model.")
    }
}
