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

tasks.register("verifyPublishing") {
    group = "verification"
    description = "Asserts every published module is correctly wired for Maven Central."
    doLast {
        val problems = mutableListOf<String>()
        val mavenPublishPluginId = "com.vanniktech.maven.publish"

        val publishedProjects = rootProject.subprojects
            .filter { it.plugins.hasPlugin(mavenPublishPluginId) }

        val platformSuffixes = setOf(
            "-jvm", "-android", "-js", "-wasmjs",
            "-iosarm64", "-iossimulatorarm64", "-metadata",
        )
        fun Project.artifactId(): String? =
            extensions.findByType(PublishingExtension::class.java)
                ?.publications
                ?.withType(MavenPublication::class.java)
                ?.firstOrNull { pub -> platformSuffixes.none { pub.artifactId.endsWith(it) } }
                ?.artifactId

        // 1. Every published project must have coordinates declared.
        publishedProjects.forEach { p ->
            if (p.artifactId() == null)
                problems += "${p.path} applies graphyn-maven-publish but has no coordinates(artifactId = ...) set."
        }

        // 2. api() and implementation() project deps on unpublished modules leak into the KMP POM.
        val publishedPaths = publishedProjects.map { it.path }.toSet()
        publishedProjects.forEach { p ->
            p.configurations
                .filter { it.name == "api" || it.name.endsWith("MainApi") ||
                           it.name == "implementation" || it.name.endsWith("MainImplementation") }
                .flatMap { cfg -> cfg.dependencies.withType(ProjectDependency::class.java).map { cfg to it } }
                .filter { (_, dep) -> dep.path !in publishedPaths }
                .forEach { (cfg, dep) ->
                    problems += "${p.path} → unpublished ${dep.path} via '${cfg.name}' (leaks into POM)."
                }
        }

        // 3. The 3 publish-config files must each mention every module/artifact.
        fun fileText(path: String) = rootProject.file(path).takeIf { it.exists() }?.readText() ?: ""
        val verifyScript = fileText("scripts/verify-maven-central.sh")
        val publishYml   = fileText(".github/workflows/publish.yml")
        val publishLocal = fileText("scripts/publish-local.sh")

        publishedProjects.forEach { p ->
            val artifactId = p.artifactId() ?: return@forEach
            if (!verifyScript.contains(artifactId))
                problems += "verify-maven-central.sh missing '$artifactId'."
            if (!publishYml.contains(p.path.removePrefix(":")))
                problems += "publish.yml missing publish step for ${p.path}."
            if (!publishLocal.contains(p.path))
                problems += "publish-local.sh PUBLISH_GROUPS missing ${p.path}."
        }

        if (problems.isNotEmpty())
            throw GradleException("Publishing audit failed:\n" + problems.joinToString("\n") { "  • $it" })

        logger.lifecycle("✓ Publishing audit passed for ${publishedProjects.size} modules.")
    }
}
