plugins {
    `kotlin-dsl`
}

// Versions must stay in sync with gradle/libs.versions.toml
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.0")
    implementation("com.android.tools.build:gradle:9.0.1")
    implementation("org.jetbrains.compose:compose-gradle-plugin:1.11.1")
}
