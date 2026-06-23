package com.ronjunevaldoz.graphyn.pluginapi

/**
 * Discovers [GraphynPlugin] implementations contributed by the platform's module system.
 *
 * On the **JVM and Android** this scans the classpath via `java.util.ServiceLoader` for
 * implementations declared in `META-INF/services/com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin`.
 * A plugin author opts in by adding that resource file listing their plugin's fully-qualified class
 * name (the class must have a public no-arg constructor or be a Kotlin `object`).
 *
 * On platforms without a runtime classpath (JS, Wasm, iOS) this returns an empty list — those hosts
 * must register plugins explicitly via [GraphynPluginRegistry.install].
 *
 * ```kotlin
 * // META-INF/services/com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
 * com.example.MyPlugin
 * ```
 */
expect fun discoverGraphynPlugins(): List<GraphynPlugin>

/**
 * Installs every plugin found by [discoverGraphynPlugins] that is not already installed
 * (matched by [GraphynPluginMetadata.id]). Returns the plugins that were newly installed.
 *
 * Combine with explicit installs — discovered plugins fill in whatever the host did not wire by hand:
 * ```kotlin
 * val registry = DefaultGraphynPluginRegistry().apply {
 *     install(CorePlugin)      // always present
 *     installDiscovered()      // plus anything on the classpath
 * }
 * ```
 */
fun GraphynPluginRegistry.installDiscovered(): List<GraphynPlugin> {
    val alreadyInstalled = plugins.mapTo(mutableSetOf()) { it.metadata.id }
    val toInstall = discoverGraphynPlugins().filter { it.metadata.id !in alreadyInstalled }
    toInstall.forEach { install(it) }
    return toInstall
}
