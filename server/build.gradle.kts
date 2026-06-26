plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
    id("graphyn-maven-publish")
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

// server sets its own project.group (com.ronjunevaldoz.graphyn) for the application
// jar, so the Maven groupId is passed explicitly here rather than inherited from the
// convention plugin's project.group default.
mavenPublishing {
    coordinates("io.github.ronjunevaldoz", "graphyn-server", libraryVersion)
    pom {
        name.set("Graphyn Server")
        description.set("Ktor server + embeddable plugin for running Graphyn workflow execution server-side")
    }
}
