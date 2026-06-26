// JVM/Desktop only — Kotlin scripting runs on the JVM classpath.
plugins {
    id("graphyn-maven-publish")
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.pluginApi)
    implementation(projects.editorApi)
    implementation(projects.ui.cards)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.kotlin.scriptingJsr223)
    testImplementation(libs.kotlin.test)
}
mavenPublishing {
    coordinates(artifactId = "graphyn-plugin-script")
    pom {
        name.set("Graphyn Plugin: script")
        description.set("First-party Graphyn plugin.")
    }
}
