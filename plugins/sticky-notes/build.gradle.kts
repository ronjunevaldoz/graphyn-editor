plugins {
    id("graphyn-maven-publish")
    id("graphyn-kmp-compose-library")
}

kotlin {
    androidLibrary {
        namespace = "com.ronjunevaldoz.graphyn.plugins.stickynotes"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.pluginApi)
            api(projects.editorApi)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
        }
    }
}
mavenPublishing {
    coordinates(artifactId = "graphyn-plugin-sticky-notes")
    pom {
        name.set("Graphyn Plugin: sticky-notes")
        description.set("First-party Graphyn plugin.")
    }
}
