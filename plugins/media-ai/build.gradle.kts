plugins {
    id("graphyn-kmp-compose-library")
    id("graphyn-maven-publish")
    alias(libs.plugins.serialization)
}

kotlin {
    androidLibrary {
        namespace = "com.ronjunevaldoz.graphyn.plugins.media-ai"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.pluginApi)
            implementation(projects.editorApi)
            implementation(projects.ui.cards)
            implementation(projects.plugins.mediaCore)
            implementation(projects.core.common)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.serialization.json)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
        }
    }
}

mavenPublishing {
    coordinates(artifactId = "graphyn-plugin-media-ai")
    pom {
        name.set("Graphyn Plugin: media-ai")
        description.set("First-party Graphyn plugin.")
    }
}