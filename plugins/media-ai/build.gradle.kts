// JVM/Desktop only: command adapters for the configured TTS, STT, and OCR providers.
plugins {
    id("graphyn-maven-publish")
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.serialization)
}

dependencies {
    api(projects.pluginApi)
    implementation(projects.editorApi)
    implementation(projects.ui.cards)
    implementation(projects.plugins.mediaCore)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.kotlinx.coroutinesCore)
    implementation(libs.serialization.json)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutinesTest)
}
mavenPublishing {
    coordinates(artifactId = "graphyn-plugin-media-ai")
    pom {
        name.set("Graphyn Plugin: media-ai")
        description.set("First-party Graphyn plugin.")
    }
}
