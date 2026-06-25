plugins {
    id("graphyn-kmp-library")
    alias(libs.plugins.serialization)
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
