plugins {
    id("graphyn-kmp-compose-library")
    id("graphyn-maven-publish")
}

kotlin {
    androidLibrary {
        namespace = "com.ronjunevaldoz.graphyn.plugins.text"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.pluginApi)
            api(projects.editorApi)
            api(projects.ui.cards)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
        }
    }
}

mavenPublishing {
    coordinates(artifactId = "graphyn-plugin-text")
    pom {
        name.set("Graphyn Plugin: text")
        description.set("First-party Graphyn plugin.")
    }
}
