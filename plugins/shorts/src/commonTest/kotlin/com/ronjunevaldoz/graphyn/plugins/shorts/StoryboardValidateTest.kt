package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StoryboardValidateTest {

    private fun sceneRecord(prompt: String?, caption: String?) = WorkflowValue.RecordValue(
        buildMap {
            if (prompt != null) put("prompt", WorkflowValue.StringValue(prompt))
            if (caption != null) put("caption", WorkflowValue.StringValue(caption))
        },
    )

    @Test
    fun malformedTopLevelFallsBackToKnownGoodStoryboard() = runTest {
        val out = storyboardValidateExecutor.execute(mapOf("input" to WorkflowValue.RecordValue(emptyMap())))
        val record = out["value"] as WorkflowValue.RecordValue
        assertEquals(WorkflowValue.StringValue("cooking"), record.fields["niche"])
        assertEquals(STORYBOARD_SCENE_COUNT, (record.fields["scenes"] as WorkflowValue.ListValue).items.size)
    }

    @Test
    fun singleMalformedSceneIsSalvagedNotDropped() = runTest {
        val input = WorkflowValue.RecordValue(
            mapOf(
                "niche" to WorkflowValue.StringValue("gardening"),
                "visual_style" to WorkflowValue.StringValue("flat vector illustration"),
                "character" to WorkflowValue.StringValue("a gardener in overalls"),
                "narration" to WorkflowValue.StringValue("Grow your own herbs at home."),
                "scenes" to WorkflowValue.ListValue(
                    listOf(
                        sceneRecord("a sunny windowsill herb garden", "Start small."),
                        sceneRecord(null, null), // dropped prompt on exactly one scene
                        sceneRecord("fresh basil in a pot", "Snip and enjoy."),
                    ),
                ),
            ),
        )
        val record = storyboardValidateExecutor.execute(mapOf("input" to input))["value"] as WorkflowValue.RecordValue
        assertEquals(WorkflowValue.StringValue("gardening"), record.fields["niche"])
        val scenes = (record.fields["scenes"] as WorkflowValue.ListValue).items
        assertEquals(STORYBOARD_SCENE_COUNT, scenes.size)
        val patched = (scenes[1] as WorkflowValue.RecordValue).fields["prompt"] as WorkflowValue.StringValue
        assertTrue(patched.value.isNotBlank())
    }
}
