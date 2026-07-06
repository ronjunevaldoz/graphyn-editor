// JVM/Desktop only: delegates to a StableDiffusionBackend (SdCliBackend by default, shells out to
// sd-cli from stable-diffusion.cpp). Swap the backend for a JNI/embedded implementation to avoid
// process-per-generation overhead — see StableDiffusionPlugin's constructor doc.
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.serialization)
    id("graphyn-maven-publish")
}

dependencies {
    api(projects.pluginApi)
    implementation(projects.core.data)
    implementation(libs.serialization.json)
    implementation(libs.kotlinx.coroutinesCore)
    implementation(libs.ktor.clientCore)
    implementation(libs.ktor.clientCio)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutinesTest)
}

mavenPublishing {
    coordinates(artifactId = "graphyn-plugin-stable-diffusion")
    pom {
        name.set("Graphyn Plugin: stable-diffusion")
        description.set(
            "First-party Graphyn plugin for stable-diffusion.cpp — sd.* node specs (diffusion, " +
                "encoders, vae, context, lora, sampler, txt2img/img2img/txt2vid/img2vid) behind a " +
                "pluggable StableDiffusionBackend so any inference runtime (CLI, JNI, remote) can " +
                "drive the same node graph.",
        )
    }
}
