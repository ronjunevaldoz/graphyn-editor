package com.ronjunevaldoz.graphyn.editor.shell.components

import com.ronjunevaldoz.graphyn.core.store.GraphynEnvironment
import com.ronjunevaldoz.graphyn.core.store.GraphynSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GraphynEnvRowsTest {
    @Test
    fun defaultRowsStayEditableAndSnakeCase() {
        val rows = rowsForEnv(GraphynSettings(), GraphynSettings.DEFAULT_ENV)
        assertEquals(listOf("sd_server_url", "sd_api_key", "ollama_host"), rows.take(3).map { it.key })
        assertTrue(rows.take(3).all { it.pinned })
    }

    @Test
    fun foldRowsNormalizesLegacyKeys() {
        val settings = GraphynSettings(environments = listOf(GraphynEnvironment("default")))
        val folded = foldRows(settings, "default", listOf(EnvRow("GRAPHYN_SD_API_KEY", "secret", pinned = false)))
        assertEquals("secret", folded.environments.first().values[GraphynSettings.KEY_SD_API_KEY])
    }
}
