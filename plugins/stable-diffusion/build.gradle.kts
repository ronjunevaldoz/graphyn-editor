// JVM/Desktop only: delegates to the sd-cli binary from stable-diffusion.cpp.
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.serialization)
}

dependencies {
    api(projects.pluginApi)
    implementation(libs.serialization.json)
    implementation(libs.kotlinx.coroutinesCore)

    testImplementation(libs.kotlin.test)
}
