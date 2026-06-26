plugins {
    id("graphyn-kmp-compose-library")
    id("graphyn-maven-publish")
}

kotlin {
    androidLibrary {
        namespace = "com.ronjunevaldoz.graphyn.plugins.types"
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
    coordinates(artifactId = "graphyn-plugin-types")
    pom {
        name.set("Graphyn Plugin: types")
        description.set("First-party Graphyn plugin.")
    }
}
