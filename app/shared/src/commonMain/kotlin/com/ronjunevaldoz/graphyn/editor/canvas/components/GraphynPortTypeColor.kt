package com.ronjunevaldoz.graphyn.editor.canvas.components

import androidx.compose.ui.graphics.Color
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType

fun PortSpec.portColor(): Color = portColor?.let { Color(it) } ?: type.portColor()

internal fun PortSpec.canvasPortColor(ownerKey: String): Color =
    portColor?.let { Color(it) } ?: derivedPortColor(ownerKey)

fun WorkflowType.portColor(): Color = when (this) {
    is WorkflowType.StringType  -> Color(0xFFA8CC8C)
    is WorkflowType.IntType     -> Color(0xFF569CD6)
    is WorkflowType.DoubleType  -> Color(0xFF4EC9B0)
    is WorkflowType.BooleanType -> Color(0xFFC586C0)
    is WorkflowType.ListType    -> Color(0xFFF8C555)
    is WorkflowType.RecordType  -> Color(0xFFCE9178)
    is WorkflowType.EnumType      -> Color(0xFFD19A66)
    is WorkflowType.MultiEnumType -> Color(0xFFE06C75)
    is WorkflowType.OpaqueType    -> Color(0xFF9B9BA5)
    is WorkflowType.NullableType -> wrappedType.portColor().copy(alpha = 0.65f)
}

private fun PortSpec.derivedPortColor(ownerKey: String): Color {
    val seed = "$ownerKey|$name|${type::class.simpleName}"
    val hash = seed.hashCode()
    val hue = ((hash ushr 1) % 360 + 360) % 360
    val saturation = (0.58f + (((hash ushr 9) and 0x7F) / 255f) * 0.14f).coerceIn(0.48f, 0.76f)
    val lightness = (0.56f + (((hash ushr 17) and 0x3F) / 255f) * 0.10f).coerceIn(0.48f, 0.68f)
    return Color.hsl(hue.toFloat(), saturation, lightness)
}
