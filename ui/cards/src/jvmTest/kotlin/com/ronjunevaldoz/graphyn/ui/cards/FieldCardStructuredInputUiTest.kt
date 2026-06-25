@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.ronjunevaldoz.graphyn.core.designsystem.theme.AppTheme
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlin.test.Test
import kotlin.test.assertEquals

class FieldCardStructuredInputUiTest {

    private val recordType = WorkflowType.RecordType(
        mapOf("name" to WorkflowType.StringType),
    )

    @Test
    fun recordFieldAcceptsAWholeWordBeforeWritingBack() = runDesktopComposeUiTest {
        var value by mutableStateOf<WorkflowValue>(
            WorkflowValue.RecordValue(mapOf("name" to WorkflowValue.StringValue("old"))),
        )
        setContent {
            AppTheme {
                FieldBody(
                    inputs = listOf(PortSpec("record", recordType)),
                    values = mapOf("record" to value),
                    onValueChange = { _, updated -> value = updated },
                    theme = FieldNodeTheme(),
                )
            }
        }

        onNodeWithText("{ 1 field } ▾").performClick()
        onNodeWithText("old").performClick()
        onNodeWithText("old").performTextReplacement("hello")
        onNodeWithText("Done").performClick()

        val fields = (value as WorkflowValue.RecordValue).fields
        assertEquals(WorkflowValue.StringValue("hello"), fields["name"])
    }

    @Test
    fun listOfRecordsUsesRecordEditorAndAcceptsAWholeWord() = runDesktopComposeUiTest {
        var value by mutableStateOf<WorkflowValue>(
            WorkflowValue.ListValue(
                listOf(WorkflowValue.RecordValue(mapOf("name" to WorkflowValue.StringValue("old")))),
            ),
        )
        setContent {
            AppTheme {
                FieldBody(
                    inputs = listOf(PortSpec("records", WorkflowType.ListType(recordType))),
                    values = mapOf("records" to value),
                    onValueChange = { _, updated -> value = updated },
                    theme = FieldNodeTheme(),
                )
            }
        }

        onNodeWithText("1 item ▾").performClick()
        onNodeWithText("old").performClick()
        onNodeWithText("old").performTextReplacement("hello")
        onNodeWithText("Done").performClick()

        val record = (value as WorkflowValue.ListValue).items.single() as WorkflowValue.RecordValue
        assertEquals(WorkflowValue.StringValue("hello"), record.fields["name"])
    }
}
