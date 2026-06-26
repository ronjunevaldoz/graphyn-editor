plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
}

group = "com.ronjunevaldoz.graphyn"

val libraryVersion = (project.findProperty("VERSION") as? String) ?: "0.5.0"
version = libraryVersion

application {
    mainClass = "com.ronjunevaldoz.graphyn.ApplicationKt"
}

dependencies {
    api(projects.pluginApi)
    api(projects.core.model)
    api(projects.core.execution)
    api(projects.core.serialization)
    api(projects.core.data)
    api(projects.runtime)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverSse)
    implementation(libs.kotlinx.coroutinesCore)
    implementation(libs.serialization.json)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    if (project.hasProperty("signing.keyId") || project.hasProperty("signingKey")) signAllPublications()
    coordinates("io.github.ronjunevaldoz", "graphyn-server", libraryVersion)
    pom {
        name = "Graphyn Server"
        description = "Ktor server + embeddable plugin for running Graphyn workflow execution server-side"
        url = "https://github.com/ronjunevaldoz/graphyn-editor"
        licenses {
            license {
                name = "Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
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
        }
    }
}
