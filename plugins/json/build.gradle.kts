plugins {
    id("graphyn-kmp-compose-library")
    id("graphyn-maven-publish")
}

kotlin {
    androidLibrary {
        namespace = "com.ronjunevaldoz.graphyn.plugins.json"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.pluginApi)
            api(projects.editorApi)
            api(projects.ui.cards)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
        }
    }
}

mavenPublishing {
    coordinates(artifactId = "graphyn-plugin-json")
    pom {
        name.set("Graphyn Plugin: json")
        description.set("First-party Graphyn plugin.")
    }
}
