// JVM/Desktop only — Kotlin scripting runs on the JVM classpath.
plugins {
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
}
