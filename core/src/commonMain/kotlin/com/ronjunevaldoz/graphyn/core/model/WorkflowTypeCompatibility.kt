package com.ronjunevaldoz.graphyn.core.model

/**
 * Type-compatibility rules for port connections.
 *
 * `OpaqueType` is universally compatible. `DoubleType` accepts `IntType` (widening). All other
 * pairs require structural equality of the type descriptor.
 */
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
        is WorkflowType.MultiEnumType ->
            actual is WorkflowType.MultiEnumType && expected.values == actual.values
        WorkflowType.OpaqueType -> true
    }
}
