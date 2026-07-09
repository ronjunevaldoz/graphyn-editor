package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComparisonMetadataTest {

    @Test
    fun metadataNodeAddsGeneratedAtWithoutDroppingResultFields() = runTest {
        val input = WorkflowValue.RecordValue(
            mapOf(
                "niche" to WorkflowValue.StringValue("coffee"),
                "visual_style" to WorkflowValue.StringValue("flat vector"),
            ),
        )

        val result = comparisonMetadataExecutor.execute(mapOf("input" to input))
        val record = result.getValue("value") as WorkflowValue.RecordValue

        assertEquals(WorkflowValue.StringValue("coffee"), record.fields["niche"])
        assertEquals(WorkflowValue.StringValue("flat vector"), record.fields["visual_style"])
        assertTrue((record.fields["generated_at"] as WorkflowValue.StringValue).value.isNotBlank())
    }
}
