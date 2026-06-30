package com.ronjunevaldoz.graphyn.core.model

/**
 * Type-compatibility rules for port connections.
 *
 * `OpaqueType` connects only to `OpaqueType` (or `NullableType(OpaqueType)`).
 * `DoubleType` accepts `IntType` (widening). All other pairs require structural equality.
 */
object WorkflowTypeCompatibility {
    fun isCompatible(expected: WorkflowType, actual: WorkflowType): Boolean =
        when (expected) {
            WorkflowType.OpaqueType -> actual is WorkflowType.OpaqueType
            WorkflowType.StringType -> actual is WorkflowType.StringType
            WorkflowType.IntType -> actual is WorkflowType.IntType
            WorkflowType.DoubleType -> actual is WorkflowType.DoubleType || actual is WorkflowType.IntType
            WorkflowType.BooleanType -> actual is WorkflowType.BooleanType
            is WorkflowType.ListType ->
                // A list source matches element-for-element; a single element also fans into the
                // list port (collected into a one-or-more-item list at execution time).
                (actual is WorkflowType.ListType && isCompatible(expected.elementType, actual.elementType)) ||
                    isCompatible(expected.elementType, actual)
            is WorkflowType.NullableType ->
                (actual is WorkflowType.NullableType &&
                    isCompatible(expected.wrappedType, actual.wrappedType)) ||
                    isCompatible(expected.wrappedType, actual)
            is WorkflowType.RecordType ->
                actual is WorkflowType.RecordType &&
                    expected.fields.size == actual.fields.size &&
                    expected.fields.all { (key, value) ->
                        actual.fields[key]?.let { isCompatible(value, it) } == true
                    }
            is WorkflowType.EnumType ->
                actual is WorkflowType.EnumType && expected.values == actual.values
            is WorkflowType.MultiEnumType ->
                actual is WorkflowType.MultiEnumType && expected.values == actual.values
        }
}
