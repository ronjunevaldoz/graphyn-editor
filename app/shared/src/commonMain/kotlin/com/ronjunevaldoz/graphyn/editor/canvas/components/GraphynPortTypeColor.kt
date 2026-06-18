package com.ronjunevaldoz.graphyn.editor.canvas.components

import androidx.compose.ui.graphics.Color
import com.ronjunevaldoz.graphyn.core.model.WorkflowType

fun WorkflowType.portColor(): Color = when (this) {
    is WorkflowType.StringType  -> Color(0xFFA8CC8C)
    is WorkflowType.IntType     -> Color(0xFF569CD6)
    is WorkflowType.DoubleType  -> Color(0xFF4EC9B0)
    is WorkflowType.BooleanType -> Color(0xFFC586C0)
    is WorkflowType.ListType    -> Color(0xFFF8C555)
    is WorkflowType.RecordType  -> Color(0xFFCE9178)
    is WorkflowType.EnumType    -> Color(0xFFD19A66)
    is WorkflowType.OpaqueType  -> Color(0xFF9B9BA5)
    is WorkflowType.NullableType -> wrappedType.portColor().copy(alpha = 0.65f)
}
