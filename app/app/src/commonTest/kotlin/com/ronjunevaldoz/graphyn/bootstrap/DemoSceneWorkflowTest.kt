@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.validation.WorkflowGraphValidator
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DemoSceneWorkflowTest {

    private val plugins = DefaultGraphynPluginRegistry().apply {
        GraphynDemoPlugins.runtime.forEach { install(it) }
    }
    private val validator = WorkflowGraphValidator(plugins.nodeSpecs)

    // --- structural integrity ---

    @Test
    fun allScenesHaveUniqueWorkflowIds() {
        val ids = WorkflowCatalog.entries.map { it.workflow.id }
        assertEquals(ids.size, ids.distinct().size, "Duplicate workflow ids: ${ids.groupBy { it }.filterValues { it.size > 1 }.keys}")
    }

    @Test
    fun allScenesHaveNonBlankNameAndId() {
        WorkflowCatalog.entries.forEach { scene ->
            assertTrue(scene.workflow.id.isNotBlank(),   "${scene.name}: workflow id is blank")
            assertTrue(scene.workflow.name.isNotBlank(), "${scene.name}: workflow name is blank")
        }
    }

    @Test
    fun allScenesHaveAtLeastOneNode() {
        WorkflowCatalog.entries.forEach { scene ->
            assertTrue(scene.workflow.nodes.isNotEmpty(), "${scene.name}: workflow has no nodes")
        }
    }

    @Test
    fun allNodeIdsWithinWorkflowAreUnique() {
        WorkflowCatalog.entries.forEach { scene ->
            val ids = scene.workflow.nodes.map { it.id }
            assertEquals(ids.size, ids.distinct().size, "${scene.name}: duplicate node ids $ids")
        }
    }

    @Test
    fun allConnectionsReferenceExistingNodeIds() {
        WorkflowCatalog.entries.forEach { scene ->
            val wf = scene.workflow
            val nodeIds = wf.nodes.map { it.id }.toSet()
            wf.connections.forEach { conn ->
                assertTrue(conn.fromNodeId in nodeIds,
                    "${scene.name}: connection from unknown node '${conn.fromNodeId}'")
                assertTrue(conn.toNodeId in nodeIds,
                    "${scene.name}: connection to unknown node '${conn.toNodeId}'")
            }
        }
    }

    // --- plugin registration ---

    // Script and media execution depend on JVM-only plugins installed by the desktop host.
    private val jvmOnlyTypes = setOf(
        "script.eval",
        "media.audio_encode",
        "media.audio_extract",
        "media.audio_mix",
        "media.audios_list",
        "media.caption_overlay",
        "media.caption_style",
        "media.image_crop",
        "media.image_import",
        "media.image_resize",
        "media.image_sequence_to_video",
        "media.images_list",
        "media.ocr",
        "media.overlays_list",
        "media.speech_to_text",
        "media.sync_point",
        "media.sync_points_list",
        "media.text_to_speech",
        "media.timing_controller",
        "media.video_compose",
        "media.video_encode",
        "media.video_import",
        "media.video_overlay",
        "media.video_stitch",
        "media.videos_list",
    )

    @Test
    fun allNodeTypesAreRegisteredInDemoPlugins() {
        WorkflowCatalog.entries.forEach { scene ->
            scene.workflow.nodes
                .filter { it.type !in jvmOnlyTypes }
                .forEach { node ->
                    assertNotNull(
                        plugins.nodeSpecs.resolve(node.type),
                        "${scene.name}: node type '${node.type}' is not registered in GraphynDemoPlugins",
                    )
                }
        }
    }

    // --- full graph validation ---

    @Test
    fun noStructuralValidationErrorsInAnyScene() {
        // Demo scenes choose nodes for narrative clarity, not production type safety:
        // - missing_required_input: ports intentionally left unwired in visual showcases
        val ignoredCodes = setOf("missing_required_input", "type_mismatch")
        val jvmOnlyScenes = setOf(
            WorkflowCatalog.Script,
            WorkflowCatalog.SimpleTts,
            WorkflowCatalog.VideoNarration,
            WorkflowCatalog.AudioMix,
            WorkflowCatalog.SmartEncode,
            WorkflowCatalog.VideoStitch,
            WorkflowCatalog.Captioned,
            WorkflowCatalog.OcrExtract,
            WorkflowCatalog.PictureInPicture,
            WorkflowCatalog.SyncCalibration,
            WorkflowCatalog.ImageEdit,
            WorkflowCatalog.Slideshow,
        )
        WorkflowCatalog.entries.forEach { scene ->
            val sceneIgnored = if (scene in jvmOnlyScenes) ignoredCodes + "unknown_node_type" else ignoredCodes
            val errors = validator.validate(scene.workflow).filterNot { it.code in sceneIgnored }
            assertTrue(errors.isEmpty(),
                "${scene.name} has ${errors.size} structural error(s):\n" +
                    errors.joinToString("\n") { "  [${it.code}] ${it.message}" }
            )
        }
    }
}
