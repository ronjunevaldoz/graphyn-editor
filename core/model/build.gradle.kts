plugins {
    id("graphyn-kmp-library")
    alias(libs.plugins.serialization)
    id("graphyn-maven-publish")
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

// Shared config (automaticRelease, signing, group, version, license/scm) lives in
// the graphyn-maven-publish convention plugin. Only the per-module identity here.
mavenPublishing {
    coordinates(artifactId = "graphyn-core-model")
    pom {
        name.set("Graphyn Core Model")
        description.set("Workflow graph model, types, values, validation, and registry — no Compose, no coroutines.")
    }
}
