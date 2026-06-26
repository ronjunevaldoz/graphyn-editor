plugins {
    id("graphyn-kmp-compose-library")
    id("graphyn-maven-publish")
}

kotlin {
    androidLibrary {
        namespace = "com.ronjunevaldoz.graphyn.plugins.preview"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.pluginApi)
            api(projects.editorApi)
            implementation(projects.core.designsystem)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
        }
    }
}

mavenPublishing {
    coordinates(artifactId = "graphyn-plugin-preview")
    pom {
        name.set("Graphyn Plugin: preview")
        description.set("First-party Graphyn plugin.")
    }
}
