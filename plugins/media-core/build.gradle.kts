// JVM/Desktop only: Phase 1 delegates media processing to local FFmpeg executables.
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
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.kotlinx.coroutinesCore)
    implementation(libs.serialization.json)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutinesTest)
}
mavenPublishing {
    coordinates(artifactId = "graphyn-plugin-media-core")
    pom {
        name.set("Graphyn Plugin: media-core")
        description.set("First-party Graphyn plugin.")
    }
}
