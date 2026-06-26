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
    ":core:designsystem", ":plugin-api", ":ai", ":editor-api", ":ui:cards",
    ":plugins:control", ":plugins:list-ops", ":plugins:types", ":plugins:text",
    ":plugins:io", ":plugins:json", ":plugins:preview",
    ":runtime", ":app:shared", ":server",
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

        // 2. No published module may have a project dependency on an unpublished module
        //    via api() OR implementation() — both leak into the KMP POM (api → compile
        //    scope, implementation → runtime scope), producing coordinates consumers
        //    cannot resolve on Maven Central.
        publishedModulePaths.mapNotNull { rootProject.findProject(it) }.forEach { p ->
            p.configurations
                .filter { cfg ->
                    cfg.name == "api" || cfg.name.endsWith("MainApi") ||
                    cfg.name == "implementation" || cfg.name.endsWith("MainImplementation")
                }
                .forEach { cfg ->
                    cfg.dependencies.withType(ProjectDependency::class.java).forEach { dep ->
                        if (dep.path !in publishedModulePaths) {
                            val via = if (cfg.name.contains("mplementation", ignoreCase = true)) "implementation" else "api"
                            problems += "${p.path} depends on unpublished ${dep.path} via '$via' ('${cfg.name}')" +
                                " — unpublished project deps always leak into the KMP POM."
                        }
                    }
                }
        }

        // 3. Verify the 3 publish-config files each mention every published module,
        //    so a new module can't be added to publishedModulePaths and then silently
        //    skipped at release time because the scripts weren't updated.
        //
        //    • verify-maven-central.sh  — artifact IDs (derived from module paths)
        //    • publish.yml              — Gradle task strings (module paths)
        //    • publish-local.sh         — module paths in PUBLISH_GROUPS
        val moduleToArtifact = mapOf(
            ":core:model" to "graphyn-core-model",
            ":core:execution" to "graphyn-core-execution",
            ":core:serialization" to "graphyn-core-serialization",
            ":core:data" to "graphyn-core-data",
            ":core:designsystem" to "graphyn-ui-design",
            ":plugin-api" to "graphyn-plugin-api",
            ":ai" to "graphyn-ai",
            ":editor-api" to "graphyn-editor-api",
            ":ui:cards" to "graphyn-ui-cards",
            ":plugins:control" to "graphyn-plugin-control",
            ":plugins:list-ops" to "graphyn-plugin-list-ops",
            ":plugins:types" to "graphyn-plugin-types",
            ":plugins:text" to "graphyn-plugin-text",
            ":plugins:io" to "graphyn-plugin-io",
            ":plugins:json" to "graphyn-plugin-json",
            ":plugins:preview" to "graphyn-plugin-preview",
            ":runtime" to "graphyn-runtime",
            ":app:shared" to "graphyn-editor",
            ":server" to "graphyn-server",
        )

        fun fileText(relativePath: String) =
            rootProject.file(relativePath).takeIf { it.exists() }?.readText() ?: ""

        val verifyScript = fileText("scripts/verify-maven-central.sh")
        val publishYml   = fileText(".github/workflows/publish.yml")
        val publishLocal = fileText("scripts/publish-local.sh")

        publishedModulePaths.forEach { path ->
            val artifactId = moduleToArtifact[path]
            if (artifactId == null) {
                problems += "$path is in publishedModulePaths but has no entry in the moduleToArtifact map in build.gradle.kts — add it."
                return@forEach
            }
            if (!verifyScript.contains(artifactId))
                problems += "scripts/verify-maven-central.sh is missing artifact '$artifactId' (module $path) — add it to ARTIFACTS."
            if (!publishYml.contains(path.removePrefix(":")))
                problems += ".github/workflows/publish.yml does not publish $path — add a publish step for it."
            if (!publishLocal.contains(path))
                problems += "scripts/publish-local.sh PUBLISH_GROUPS is missing $path — add it in dependency order."
        }

        if (problems.isNotEmpty()) {
            throw GradleException(
                "Publishing audit failed:\n" + problems.joinToString("\n") { "  • $it" },
            )
        }
        logger.lifecycle("✓ Publishing audit passed for ${publishedModulePaths.size} modules.")
    }
}
