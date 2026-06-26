plugins {
    id("graphyn-kmp-compose-library")
    id("graphyn-maven-publish")
}

kotlin {
    androidLibrary {
        namespace = "com.ronjunevaldoz.graphyn.plugins.listops"
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
    coordinates(artifactId = "graphyn-plugin-list-ops")
    pom {
        name.set("Graphyn Plugin: list-ops")
        description.set("First-party Graphyn plugin.")
    }
}
