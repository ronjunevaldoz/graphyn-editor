import org.gradle.api.tasks.JavaExec

plugins {
    id("graphyn-kmp-library")
    alias(libs.plugins.serialization)
    id("graphyn-maven-publish")
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

mavenPublishing {
    coordinates(artifactId = "graphyn-core-execution")
    pom {
        name.set("Graphyn Core Execution")
        description.set("Workflow execution engine, executors, scheduling, and events for the Graphyn model.")
    }
}
