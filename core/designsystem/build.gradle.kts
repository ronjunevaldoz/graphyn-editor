plugins {
    id("graphyn-kmp-compose-library")
    alias(libs.plugins.dokka)
}

kotlin {
    androidLibrary {
        namespace = "com.ronjunevaldoz.graphyn.core.designsystem"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
