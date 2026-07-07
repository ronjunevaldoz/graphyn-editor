package com.ronjunevaldoz.graphyn.editor.canvas

import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.editor.canvas.components.portColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GraphynPortTypeColorTest {

    // One instance per concrete WorkflowType variant. Extend this list when adding a variant —
    // the uniqueness test below is the guard that its color doesn't silently collide.
    private val baseTypes = listOf(
        WorkflowType.StringType,
        WorkflowType.IntType,
        WorkflowType.DoubleType,
        WorkflowType.BooleanType,
        WorkflowType.ListType(WorkflowType.StringType),
        WorkflowType.RecordType(emptyMap()),
        WorkflowType.EnumType(listOf("a")),
        WorkflowType.MultiEnumType(listOf("a")),
        WorkflowType.OpaqueType,
    )

    @Test
    fun everyWorkflowTypeHasAUniquePortColor() {
        val colors = baseTypes.associateWith { it.portColor() }
        val collisions = colors.entries.groupBy({ it.value }, { it.key }).filterValues { it.size > 1 }
        assertTrue(
            collisions.isEmpty(),
            "port colors must be unique per type; collisions: ${collisions.values.map { types -> types.map { it::class.simpleName } }}",
        )
    }

    @Test
    fun nullableTypeIsDistinguishableFromItsWrappedType() {
        baseTypes.forEach { type ->
            assertNotEquals(
                type.portColor(),
                WorkflowType.NullableType(type).portColor(),
                "nullable ${type::class.simpleName} must not share its wrapped type's exact color",
            )
        }
    }

    @Test
    fun portSpecColorOverrideWinsOverTypeColor() {
        val spec = PortSpec(name = "latent", type = WorkflowType.OpaqueType, portColor = 0xFF123456)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF123456), spec.portColor())
    }
}
