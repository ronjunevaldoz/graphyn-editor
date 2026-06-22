plugins {
    id("graphyn-kmp-library")
}

kotlin {
    androidLibrary {
        namespace = "com.ronjunevaldoz.graphyn.runtime"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.pluginApi)
            api(projects.editorApi)
            api(projects.plugins.control)
            api(projects.plugins.listOps)
            api(projects.plugins.types)
            api(projects.plugins.text)
            api(projects.plugins.io)
            api(projects.plugins.json)
            api(projects.plugins.preview)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
