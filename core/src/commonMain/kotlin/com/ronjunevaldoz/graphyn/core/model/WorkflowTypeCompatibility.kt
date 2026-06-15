package com.ronjunevaldoz.graphyn.core.model

object WorkflowTypeCompatibility {
    fun isCompatible(expected: WorkflowType, actual: WorkflowType): Boolean = when (expected) {
        WorkflowType.StringType -> actual is WorkflowType.StringType
        WorkflowType.IntType -> actual is WorkflowType.IntType
        WorkflowType.DoubleType -> actual is WorkflowType.DoubleType || actual is WorkflowType.IntType
        WorkflowType.BooleanType -> actual is WorkflowType.BooleanType
        is WorkflowType.ListType ->
            actual is WorkflowType.ListType &&
                isCompatible(expected.elementType, actual.elementType)
        is WorkflowType.NullableType ->
            (actual is WorkflowType.NullableType &&
                isCompatible(expected.wrappedType, actual.wrappedType)) ||
                isCompatible(expected.wrappedType, actual)
        is WorkflowType.RecordType ->
            actual is WorkflowType.RecordType &&
                expected.fields.size == actual.fields.size &&
                expected.fields.all { (key, value) ->
                    actual.fields[key]?.let { actualField ->
                        isCompatible(value, actualField)
                    } == true
                }
        is WorkflowType.EnumType ->
            actual is WorkflowType.EnumType && expected.values == actual.values
        WorkflowType.OpaqueType -> actual is WorkflowType.OpaqueType
    }
}
