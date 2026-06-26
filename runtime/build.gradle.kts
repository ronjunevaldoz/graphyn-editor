plugins {
    id("graphyn-kmp-library")
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
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

val libraryVersion = (project.findProperty("VERSION") as? String) ?: "0.6.0"

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    if (project.hasProperty("signing.keyId") || project.hasProperty("signingKey")) signAllPublications()
    coordinates("io.github.ronjunevaldoz", "graphyn-runtime", libraryVersion)
    pom {
        name = "Graphyn Runtime"
        description = "Convenience bundle of all first-party Graphyn plugins — control, list-ops, types, text, io, json, preview"
        url = "https://github.com/ronjunevaldoz/graphyn-editor"
        licenses {
            license {
                name = "Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "ronjunevaldoz"
                name = "Ron June Valdoz"
                email = "ronjune.lopez@gmail.com"
            }
        }
        scm {
            url = "https://github.com/ronjunevaldoz/graphyn-editor"
            connection = "scm:git:git://github.com/ronjunevaldoz/graphyn-editor.git"
            developerConnection = "scm:git:ssh://git@github.com/ronjunevaldoz/graphyn-editor.git"
        }
    }
}
