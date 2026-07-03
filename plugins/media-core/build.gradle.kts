// JVM/Desktop only: shells out to FFmpeg on PATH. Only a jvmMain actual exists — Android, iOS,
// JS, and WasmJS have no FFmpeg CLI to shell out to, so this deliberately doesn't declare those
// targets (previously used the full graphyn-kmp-compose-library convention plugin, which declared
// all 6 targets despite only ever having a JVM actual — broke compileAndroidMain/iOS/JS/WasmJS).
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
    implementation(projects.core.common)
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