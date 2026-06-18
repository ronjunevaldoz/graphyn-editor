package com.ronjunevaldoz.graphyn.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface WorkflowType {
    @Serializable
    @SerialName("string")
    data object StringType : WorkflowType

    @Serializable
    @SerialName("int")
    data object IntType : WorkflowType

    @Serializable
    @SerialName("double")
    data object DoubleType : WorkflowType

    @Serializable
    @SerialName("boolean")
    data object BooleanType : WorkflowType

    @Serializable
    @SerialName("list")
    data class ListType(val elementType: WorkflowType) : WorkflowType

    @Serializable
    @SerialName("nullable")
    data class NullableType(val wrappedType: WorkflowType) : WorkflowType

    @Serializable
    @SerialName("record")
    data class RecordType(val fields: Map<String, WorkflowType>) : WorkflowType

    @Serializable
    @SerialName("enum")
    data class EnumType(val values: List<String>) : WorkflowType

    @Serializable
    @SerialName("multi-enum")
    data class MultiEnumType(val values: List<String>) : WorkflowType

    @Serializable
    @SerialName("opaque")
    data object OpaqueType : WorkflowType
}
