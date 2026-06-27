plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("graphyn-maven-publish")
}

kotlin {
    jvm()
    js {
        browser()
    }
    wasmJs {
        browser()
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.pluginApi)
            implementation(projects.ui.cards)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.ktor.clientCore)
            implementation(libs.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.clientCio)
        }

        jsMain.dependencies {
            implementation(libs.ktor.clientJs)
        }

        wasmJsMain.dependencies {
            implementation(libs.ktor.clientJs)
        }

        iosMain.dependencies {
            implementation(libs.ktor.clientDarwin)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
        }
    }
}
mavenPublishing {
    coordinates(artifactId = "graphyn-plugin-linkedin")
    pom {
        name.set("Graphyn Plugin: linkedin")
        description.set("First-party Graphyn plugin.")
    }
}
