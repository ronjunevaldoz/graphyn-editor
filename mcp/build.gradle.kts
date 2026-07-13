plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.serialization)
    application
}

application {
    mainClass = "com.ronjunevaldoz.graphyn.mcp.MainKt"
}

dependencies {
    // :server exposes core.model/core.data/core.serialization/core.execution as `api`, so
    // createGraphynServerRuntime(), FileWorkflowStore, DefaultWorkflowJsonCodec, WorkflowValue,
    // etc. are all already on the compile classpath here.
    implementation(projects.server)
    implementation(projects.plugins.shorts)
    implementation(projects.plugins.mediaCore)
    implementation(projects.plugins.mediaAi)
    implementation(projects.plugins.stableDiffusion)
    implementation(libs.kotlinx.coroutinesCore)
    implementation(libs.serialization.json)
    implementation(libs.mcp.sdkServer)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.kotlinx.coroutinesTest)
}
