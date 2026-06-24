rootProject.name = "Graphyn"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":app:androidApp")
include(":app:demo")
include(":app:desktopApp")
include(":app:sample")
include(":app:shared")
include(":app:webApp")
include(":ai")
include(":core")
include(":core:designsystem")
include(":runtime")
include(":editor-api")
include(":plugin-api")
include(":plugins:sample-logger")
include(":plugins:sample-logger-ui")
include(":plugins:sample-math")
include(":plugins:sample-style-nodes")
include(":plugins:sticky-notes")
include(":plugins:list-ops")
include(":plugins:control")
include(":plugins:types")
include(":plugins:text")
include(":plugins:io")
include(":plugins:json")
include(":plugins:script")
include(":plugins:preview")
include(":plugins:gmail")
include(":server")
include(":ui:cards")
