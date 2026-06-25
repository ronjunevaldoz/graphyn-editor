import org.gradle.api.tasks.JavaExec

plugins {
    id("graphyn-kmp-library")
    alias(libs.plugins.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    androidLibrary {
        namespace = "com.ronjunevaldoz.graphyn.core.execution"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            implementation(libs.serialization.json)
            implementation(libs.kotlinx.coroutinesCore)
        }
        commonTest.dependencies {
            // CoreWorkflowTest is a full-stack integration test (build → validate →
            // execute → serialize round-trip), so it needs the serialization module too.
            implementation(projects.core.serialization)
            implementation(libs.serialization.json)
        }
    }
}

tasks.register<JavaExec>("benchmarkCore") {
    group = "verification"
    description = "Runs the Graphyn core benchmark snapshot."
    dependsOn("jvmTestClasses")
    mainClass.set("com.ronjunevaldoz.graphyn.core.benchmark.CoreBenchmarkKt")
    classpath = files(
        layout.buildDirectory.dir("classes/kotlin/jvm/test"),
        configurations.getByName("jvmTestRuntimeClasspath"),
    )
}

val libraryVersion = (project.findProperty("VERSION") as? String) ?: "0.2.1"

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    if (project.hasProperty("signing.keyId") || project.hasProperty("signingKey")) signAllPublications()
    coordinates("io.github.ronjunevaldoz", "graphyn-core-execution", libraryVersion)
    pom {
        name = "Graphyn Core Execution"
        description = "Workflow execution engine, executors, scheduling, and events for the Graphyn model."
        url = "https://github.com/ronjunevaldoz/graphyn-editor"
        licenses { license { name = "Apache License, Version 2.0"; url = "https://www.apache.org/licenses/LICENSE-2.0.txt" } }
        developers { developer { id = "ronjunevaldoz"; name = "Ron June Valdoz"; email = "ronjune.lopez@gmail.com" } }
        scm {
            url = "https://github.com/ronjunevaldoz/graphyn-editor"
            connection = "scm:git:git://github.com/ronjunevaldoz/graphyn-editor.git"
            developerConnection = "scm:git:ssh://git@github.com/ronjunevaldoz/graphyn-editor.git"
        }
    }
}
