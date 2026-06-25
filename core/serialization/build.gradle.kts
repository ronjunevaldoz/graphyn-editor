plugins {
    id("graphyn-kmp-library")
    alias(libs.plugins.serialization)
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
