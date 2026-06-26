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

// ── Publishing audit (preflight) ──────────────────────────────────────────────
// The canonical set of modules that publish to Maven Central. Keep in sync with
// .github/workflows/publish.yml and scripts/publish-local.sh.
val publishedModulePaths = setOf(
    ":core:model", ":core:execution", ":core:serialization", ":core:data",
    ":plugin-api", ":ai", ":editor-api", ":runtime", ":ui:cards", ":app:shared", ":server",
)

// Run in CI on every PR. Fails fast if a published module is misconfigured, so a
// release never silently ships a broken or POM-leaking artifact again.
tasks.register("verifyPublishing") {
    group = "verification"
    description = "Asserts every published module is correctly wired for Maven Central."
    doLast {
        val problems = mutableListOf<String>()

        // 1. Every expected module must exist and apply the convention plugin, which
        //    is the single source of automaticRelease=true + the signing guard.
        publishedModulePaths.forEach { path ->
            val p = rootProject.findProject(path)
            when {
                p == null ->
                    problems += "$path is listed as published but is not in the build."
                !p.plugins.hasPlugin("com.vanniktech.maven.publish") ->
                    problems += "$path does not apply graphyn-maven-publish (no automaticRelease/signing guarantee)."
            }
        }

        // 2. No published module may have an api() project dependency on an unpublished
        //    module — it leaks into the POM as a coordinate consumers cannot resolve.
        publishedModulePaths.mapNotNull { rootProject.findProject(it) }.forEach { p ->
            p.configurations
                .filter { it.name == "api" || it.name.endsWith("MainApi") }
                .forEach { cfg ->
                    cfg.dependencies.withType(ProjectDependency::class.java).forEach { dep ->
                        if (dep.path !in publishedModulePaths) {
                            problems += "${p.path} api-depends on unpublished ${dep.path} via '${cfg.name}'" +
                                " — use implementation() so it stays off the POM."
                        }
                    }
                }
        }

        if (problems.isNotEmpty()) {
            throw GradleException(
                "Publishing audit failed:\n" + problems.joinToString("\n") { "  • $it" },
            )
        }
        logger.lifecycle("✓ Publishing audit passed for ${publishedModulePaths.size} modules.")
    }
}
