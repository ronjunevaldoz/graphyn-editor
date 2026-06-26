plugins {
    id("graphyn-kmp-compose-library")
    id("graphyn-maven-publish")
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

mavenPublishing {
    coordinates(artifactId = "graphyn-ui-design")
    pom {
        name.set("Graphyn UI Design System")
        description.set("Compose Multiplatform design tokens, theme, and UI primitives for Graphyn.")
    }
}
