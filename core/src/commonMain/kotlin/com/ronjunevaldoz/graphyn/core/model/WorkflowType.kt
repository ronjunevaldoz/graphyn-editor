package com.ronjunevaldoz.graphyn.core.model

sealed interface WorkflowType {
    data object StringType : WorkflowType
    data object IntType : WorkflowType
    data object DoubleType : WorkflowType
    data object BooleanType : WorkflowType

    data class ListType(val elementType: WorkflowType) : WorkflowType
    data class NullableType(val wrappedType: WorkflowType) : WorkflowType
    data class RecordType(val fields: Map<String, WorkflowType>) : WorkflowType
    data class EnumType(val values: List<String>) : WorkflowType
    data object OpaqueType : WorkflowType
}
