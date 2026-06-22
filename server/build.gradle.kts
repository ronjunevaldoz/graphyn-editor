plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
}

group = "com.ronjunevaldoz.graphyn"
version = "1.0.0"
application {
    mainClass = "com.ronjunevaldoz.graphyn.ApplicationKt"
}

dependencies {
    api(projects.pluginApi)
    api(projects.core)
    api(projects.runtime)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.serialization.json)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}
