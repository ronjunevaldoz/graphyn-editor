package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RoborazziOptions
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.DefaultNodeCategoryRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryMeta
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

/**
 * Verifies the neutralised node browser: category markers are monochrome (no per-brand colour),
 * and Gmail/LinkedIn nest under a single "Socials" folder.
 */
class GraphynPalettePanelUiTest {

    private val roborazziOptions = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0F),
    )

    private fun fixtures(): Pair<DefaultNodeSpecRegistry, DefaultNodeCategoryRegistry> {
        fun node(type: String, label: String, category: String) =
            NodeSpec(type = type, label = label, inputs = emptyList(), outputs = emptyList(), category = category)
        val specs = DefaultNodeSpecRegistry().apply {
            register(node("gmail.fetch", "Fetch Emails", "graphyn.gmail"))
            register(node("gmail.send", "Send Email", "graphyn.gmail"))
            register(node("linkedin.post", "Share Post", "graphyn.linkedin"))
            register(node("text.upper", "Uppercase", "graphyn.text"))
            register(node("json.parse", "Parse JSON", "graphyn.json"))
            register(node("control.if", "If", "graphyn.control"))
            register(node("ai.sampler", "KSampler", "graphyn.ai"))
            register(node("io.http", "HTTP Request", "graphyn.io"))
        }
        val cats = DefaultNodeCategoryRegistry().apply {
            register("graphyn.gmail", NodeCategoryMeta("Gmail", 0xFF4285F4, group = "Socials"))
            register("graphyn.linkedin", NodeCategoryMeta("LinkedIn", 0xFF0A66C2, group = "Socials"))
            register("graphyn.text", NodeCategoryMeta("Text", 0xFF818CF8, group = "Data"))
            register("graphyn.json", NodeCategoryMeta("JSON", 0xFFFBBF24, group = "Data"))
            register("graphyn.control", NodeCategoryMeta("Control", 0xFFF9A825, group = "Flow"))
            register("graphyn.ai", NodeCategoryMeta("AI", 0xFF6B6BF7, group = "Creative"))
            register("graphyn.io", NodeCategoryMeta("I/O", 0xFF34D399))
        }
        return specs to cats
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun palette_socialsFolderExpanded() = runDesktopComposeUiTest {
        val (specs, cats) = fixtures()
        setContent {
            Box(Modifier.width(240.dp)) {
                GraphynPalettePanel(modifier = Modifier, nodeSpecs = specs, categoryRegistry = cats, onAddNode = {})
            }
        }
        // Drill in: open Socials → Gmail so the nesting is visible in the capture.
        onNodeWithText("Socials").performClick()
        onNodeWithText("Gmail").performClick()
        onRoot().captureRoboImage(roborazziOptions = roborazziOptions)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun palette_organizedTopLevel() = runDesktopComposeUiTest {
        val (specs, cats) = fixtures()
        setContent {
            Box(Modifier.width(240.dp)) {
                GraphynPalettePanel(modifier = Modifier, nodeSpecs = specs, categoryRegistry = cats, onAddNode = {})
            }
        }
        // All folders collapsed — shows the organized top level: Creative, Data, Flow, Socials, then I/O.
        onRoot().captureRoboImage(roborazziOptions = roborazziOptions)
    }
}
