package com.ronjunevaldoz.graphyn.core

import com.ronjunevaldoz.graphyn.core.store.GraphynEnvironment
import com.ronjunevaldoz.graphyn.core.store.GraphynSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GraphynSettingsTest {

    @Test
    fun valueReadsActiveEnvironment() {
        val s = GraphynSettings(
            activeEnvironment = "prod",
            environments = listOf(
                GraphynEnvironment("dev", mapOf(GraphynSettings.KEY_SD_URL to "http://dev")),
                GraphynEnvironment("prod", mapOf(GraphynSettings.KEY_SD_URL to "http://prod")),
            ),
        )
        assertEquals("http://prod", s.value(GraphynSettings.KEY_SD_URL))
        assertNull(s.value("MISSING"))
    }

    @Test
    fun legacyFieldsMigrateIntoActiveEnvironment() {
        val legacy = GraphynSettings(sdServerUrl = "http://old", sdApiKey = "k").migrated()
        assertEquals("http://old", legacy.value(GraphynSettings.KEY_SD_URL))
        assertEquals("k", legacy.value(GraphynSettings.KEY_SD_API_KEY))
        // legacy fields folded into the default environment
        assertEquals("http://old", legacy.environments.first().values[GraphynSettings.KEY_SD_URL])
    }

    @Test
    fun legacyEnvironmentKeysStillResolve() {
        val settings = GraphynSettings(
            environments = listOf(GraphynEnvironment("default", mapOf("GRAPHYN_SD_SERVER_URL" to "http://old"))),
        )
        assertEquals("http://old", settings.value(GraphynSettings.KEY_SD_URL))
        assertEquals("http://old", settings.migrated().environments.first().values[GraphynSettings.KEY_SD_URL])
    }

    @Test
    fun existingEnvironmentValueWinsOverLegacy() {
        val s = GraphynSettings(
            environments = listOf(GraphynEnvironment("default", mapOf(GraphynSettings.KEY_SD_URL to "http://new"))),
            sdServerUrl = "http://old",
        ).migrated()
        assertEquals("http://new", s.value(GraphynSettings.KEY_SD_URL))
    }
}
