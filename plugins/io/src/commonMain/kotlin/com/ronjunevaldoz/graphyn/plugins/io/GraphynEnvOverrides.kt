package com.ronjunevaldoz.graphyn.plugins.io

import kotlin.concurrent.Volatile

/**
 * A resolver consulted by [EnvReader] before the real process environment. The app points
 * [provider] at the active settings environment, so a workflow's `env` node (and anything else
 * reading through [EnvReader]) picks up custom key-values — and switching environment applies live,
 * without a restart. Default provider returns null, so plain `System.getenv` behaviour is preserved.
 */
object GraphynEnvOverrides {
    @Volatile
    var provider: (String) -> String? = { null }
}
