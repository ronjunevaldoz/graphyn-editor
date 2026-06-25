plugins {
    id("graphyn-kmp-library")
    alias(libs.plugins.serialization)
}

kotlin {
    androidLibrary {
        namespace = "com.ronjunevaldoz.graphyn.core.execution"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            implementation(libs.serialization.json)
            implementation(libs.kotlinx.coroutinesCore)
        }
    }
}
