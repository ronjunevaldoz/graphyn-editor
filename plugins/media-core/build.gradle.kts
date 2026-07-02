plugins {
    id("graphyn-kmp-compose-library")
    id("graphyn-maven-publish")
    alias(libs.plugins.serialization)
}

kotlin {
    androidLibrary {
        namespace = "com.ronjunevaldoz.graphyn.plugins.media-core"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.pluginApi)
            implementation(projects.editorApi)
            implementation(projects.ui.cards)
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
    coordinates(artifactId = "graphyn-plugin-media-core")
    pom {
        name.set("Graphyn Plugin: media-core")
        description.set("First-party Graphyn plugin.")
    }
}