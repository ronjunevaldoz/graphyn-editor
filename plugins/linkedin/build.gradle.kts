plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.mavenPublish)
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
            implementation(project(":core"))
            implementation(project(":editor-api"))
            implementation(project(":plugin-api"))
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.ktor.clientCore)
            implementation(libs.serialization.json)
            implementation(libs.kotlinx.datetime)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.clientCio)
            implementation(project(":ui:cards"))
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
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
