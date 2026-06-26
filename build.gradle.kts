plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.mavenPublish) apply false
}

// module path → Maven artifact ID. Single source of truth for the publish audit.
// Keep publish order in publish.yml / publish-local.sh consistent with dep order.
val publishedModules = mapOf(
    ":core:model"           to "graphyn-core-model",
    ":core:execution"       to "graphyn-core-execution",
    ":core:serialization"   to "graphyn-core-serialization",
    ":core:data"            to "graphyn-core-data",
    ":core:designsystem"    to "graphyn-ui-design",
    ":plugin-api"           to "graphyn-plugin-api",
    ":ai"                   to "graphyn-ai",
    ":editor-api"           to "graphyn-editor-api",
    ":ui:cards"             to "graphyn-ui-cards",
    ":plugins:control"      to "graphyn-plugin-control",
    ":plugins:list-ops"     to "graphyn-plugin-list-ops",
    ":plugins:types"        to "graphyn-plugin-types",
    ":plugins:text"         to "graphyn-plugin-text",
    ":plugins:io"           to "graphyn-plugin-io",
    ":plugins:json"         to "graphyn-plugin-json",
    ":plugins:preview"      to "graphyn-plugin-preview",
    ":plugins:sticky-notes" to "graphyn-plugin-sticky-notes",
    ":plugins:script"       to "graphyn-plugin-script",
    ":plugins:media-core"   to "graphyn-plugin-media-core",
    ":plugins:media-ai"     to "graphyn-plugin-media-ai",
    ":plugins:gmail"        to "graphyn-plugin-gmail",
    ":plugins:linkedin"     to "graphyn-plugin-linkedin",
    ":runtime"              to "graphyn-runtime",
    ":app:shared"           to "graphyn-editor",
    ":server"               to "graphyn-server",
)

tasks.register("verifyPublishing") {
    group = "verification"
    description = "Asserts every published module is correctly wired for Maven Central."
    doLast {
        val problems = mutableListOf<String>()
        val mavenPublishPluginId = "com.vanniktech.maven.publish"

        fun Project.isMavenPublishApplied() = plugins.hasPlugin(mavenPublishPluginId)

        // 1. Convention plugin applied ↔ registered in publishedModules (both directions).
        publishedModules.keys.forEach { path ->
            val p = rootProject.findProject(path)
            when {
                p == null -> problems += "$path is in publishedModules but not in the build."
                !p.isMavenPublishApplied() -> problems += "$path is missing graphyn-maven-publish."
            }
        }
        rootProject.subprojects.filter { it.isMavenPublishApplied() }.forEach { p ->
            if (p.path !in publishedModules)
                problems += "${p.path} applies graphyn-maven-publish but is missing from publishedModules — add it."
        }

        // 2. api() and implementation() project deps on unpublished modules leak into the KMP POM.
        publishedModules.keys.mapNotNull { rootProject.findProject(it) }.forEach { p ->
            p.configurations
                .filter { it.name == "api" || it.name.endsWith("MainApi") ||
                           it.name == "implementation" || it.name.endsWith("MainImplementation") }
                .flatMap { cfg -> cfg.dependencies.withType(ProjectDependency::class.java).map { cfg to it } }
                .filter { (_, dep) -> dep.path !in publishedModules }
                .forEach { (cfg, dep) ->
                    problems += "${p.path} → unpublished ${dep.path} via '${cfg.name}' (leaks into POM)."
                }
        }

        // 3. The 3 publish-config files must each mention every module/artifact.
        fun fileText(path: String) = rootProject.file(path).takeIf { it.exists() }?.readText() ?: ""
        val verifyScript = fileText("scripts/verify-maven-central.sh")
        val publishYml   = fileText(".github/workflows/publish.yml")
        val publishLocal = fileText("scripts/publish-local.sh")

        publishedModules.forEach { (path, artifactId) ->
            if (!verifyScript.contains(artifactId))
                problems += "verify-maven-central.sh missing '$artifactId'."
            if (!publishYml.contains(path.removePrefix(":")))
                problems += "publish.yml missing publish step for $path."
            if (!publishLocal.contains(path))
                problems += "publish-local.sh PUBLISH_GROUPS missing $path."
        }

        if (problems.isNotEmpty())
            throw GradleException("Publishing audit failed:\n" + problems.joinToString("\n") { "  • $it" })

        logger.lifecycle("✓ Publishing audit passed for ${publishedModules.size} modules.")
    }
}
