plugins {
    id("graphyn-kmp-library")
    id("graphyn-maven-publish")
}

kotlin {
    androidLibrary {
        namespace = "com.ronjunevaldoz.graphyn.runtime"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.pluginApi)
            api(projects.editorApi)
            // implementation keeps these off the published POM — they are not on Maven Central.
            // Consumers of graphyn-server get the plugin behaviour through the server's bundled classpath.
            implementation(projects.plugins.control)
            implementation(projects.plugins.listOps)
            implementation(projects.plugins.types)
            implementation(projects.plugins.text)
            implementation(projects.plugins.io)
            implementation(projects.plugins.json)
            implementation(projects.plugins.preview)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

mavenPublishing {
    coordinates(artifactId = "graphyn-runtime")
    pom {
        name.set("Graphyn Runtime")
        description.set("Convenience bundle of all first-party Graphyn plugins — control, list-ops, types, text, io, json, preview")
    }
}
