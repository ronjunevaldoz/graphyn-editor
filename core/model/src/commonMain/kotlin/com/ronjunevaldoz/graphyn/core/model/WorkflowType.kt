package com.ronjunevaldoz.graphyn.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Describes the type of a port or a value in the workflow graph.
 *
 * Use [WorkflowTypeCompatibility.isCompatible] to test whether an output type
 * can connect to an input type. The rules are:
 * - Primitive types are exact-match only, except [DoubleType] which also accepts [IntType].
 * - [ListType] matches a list with compatible element types, or a single element-compatible
 *   value (which fans into the list).
 * - [NullableType] unwraps: a non-nullable actual is compatible with a nullable expected
 *   if the inner types are compatible.
 * - [RecordType] matches field-for-field by key and type.
 * - [EnumType] and [MultiEnumType] require identical value lists.
 * - [OpaqueType] accepts any incoming type — use for runtime handles the model should not inspect.
 */
@Serializable
sealed interface WorkflowType {
    @Serializable @SerialName("string")   data object StringType  : WorkflowType
    @Serializable @SerialName("int")      data object IntType     : WorkflowType
    @Serializable @SerialName("double")   data object DoubleType  : WorkflowType
    @Serializable @SerialName("boolean")  data object BooleanType : WorkflowType

    /** A homogeneous ordered list. Connection validation checks [elementType] recursively. */
    @Serializable @SerialName("list")
    data class ListType(val elementType: WorkflowType) : WorkflowType

    /** Wraps any type; a non-nullable actual is still compatible with a nullable port. */
    @Serializable @SerialName("nullable")
    data class NullableType(val wrappedType: WorkflowType) : WorkflowType

    /** Structured record with named, typed fields. All keys and types must match. */
    @Serializable @SerialName("record")
    data class RecordType(val fields: Map<String, WorkflowType>) : WorkflowType

    /** Single-select enum. [values] must match exactly for a connection to be valid. */
    @Serializable @SerialName("enum")
    data class EnumType(val values: List<String>) : WorkflowType

    /** Multi-select enum stored as a [WorkflowValue.ListValue] of [WorkflowValue.StringValue]. */
    @Serializable @SerialName("multi-enum")
    data class MultiEnumType(val values: List<String>) : WorkflowType

    /** Pass-through type that accepts any incoming connection. Use for untyped runtime handles. */
    @Serializable @SerialName("opaque")
    data object OpaqueType : WorkflowType
}

/**
 * The element type if this is a list port — unwrapping a [WorkflowType.NullableType] wrapper —
 * or null if the port is not a list. Drives list-port fan-in: multiple single-element connections
 * (or one list connection) collapse into one [WorkflowValue.ListValue] for the port.
 */
fun WorkflowType.listElementType(): WorkflowType? = when (this) {
    is WorkflowType.ListType -> elementType
    is WorkflowType.NullableType -> wrappedType.listElementType()
    else -> null
}

/** Human-readable label for a type, shown in port tooltips and the inspector. */
fun WorkflowType.displayName(): String = when (this) {
    WorkflowType.StringType        -> "String"
    WorkflowType.IntType           -> "Int"
    WorkflowType.DoubleType        -> "Double"
    WorkflowType.BooleanType       -> "Boolean"
    WorkflowType.OpaqueType        -> "Any"
    is WorkflowType.ListType       -> "List<${elementType.displayName()}>"
    is WorkflowType.NullableType   -> "${wrappedType.displayName()}?"
    is WorkflowType.RecordType     -> "Record"
    is WorkflowType.EnumType       -> "Enum"
    is WorkflowType.MultiEnumType  -> "MultiEnum"
}
