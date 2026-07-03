// JVM/Desktop only: TTS/STT/OCR adapters shelling out to platform CLIs (macOS `say`, tesseract)
// and depending on plugins/media-core (also JVM-only, FFmpeg-backed). Only jvmMain actuals ever
// existed for 5 of 6 expect declarations (the 6th had a "not supported on this platform" web
// stub) — previously declared all 6 KMP targets via graphyn-kmp-compose-library regardless.
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
    implementation(projects.core.common)
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