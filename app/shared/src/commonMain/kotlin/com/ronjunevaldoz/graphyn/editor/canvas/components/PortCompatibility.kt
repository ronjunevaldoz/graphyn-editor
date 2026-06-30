package com.ronjunevaldoz.graphyn.editor.canvas.components

import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowTypeCompatibility

/**
 * Port-level compatibility check. Enforces both type AND portColor matching for OpaqueType ports.
 *
 * OpaqueType ports use portColor as a semantic channel identifier — `COLOR_MODEL` ports only
 * connect to other `COLOR_MODEL` ports, preventing cross-channel wiring at the editor level.
 */
object PortCompatibility {

    fun isCompatible(expected: PortSpec, actual: PortSpec): Boolean =
        WorkflowTypeCompatibility.isCompatible(expected.type, actual.type) &&
            opaqueColorsMatch(expected, actual)

    fun opaqueColorsMatch(a: PortSpec, b: PortSpec): Boolean {
        if (!isOpaquePort(a) || !isOpaquePort(b)) return true
        return a.portColor == b.portColor
    }

    private fun isOpaquePort(port: PortSpec): Boolean {
        val t = port.type
        return t is WorkflowType.OpaqueType ||
            (t is WorkflowType.NullableType && t.wrappedType is WorkflowType.OpaqueType)
    }
}
