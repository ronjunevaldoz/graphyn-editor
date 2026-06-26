plugins {
    `kotlin-dsl`
}

// Versions must stay in sync with gradle/libs.versions.toml
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.0")
    implementation("com.android.tools.build:gradle:9.0.1")
    implementation("org.jetbrains.compose:compose-gradle-plugin:1.11.1")
    // Required so the graphyn-maven-publish convention plugin can apply these by id.
    implementation("com.vanniktech:gradle-maven-publish-plugin:0.37.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.0.0")
}
