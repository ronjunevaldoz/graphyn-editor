package com.ronjunevaldoz.graphyn.bootstrap

/** Returns the first non-blank env var among [names], checked in order. */
internal fun envValue(vararg names: String): String? =
    names.asSequence().mapNotNull { System.getenv(it)?.ifBlank { null } }.firstOrNull()
