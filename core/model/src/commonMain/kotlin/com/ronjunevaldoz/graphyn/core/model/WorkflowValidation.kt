package com.ronjunevaldoz.graphyn.core.model

import kotlinx.serialization.Serializable

/** A single validation failure produced by a [WorkflowValidator]. */
@Serializable
data class ValidationError(
    /** Machine-readable error code (e.g. `"type_mismatch"`, `"missing_required_input"`). */
    val code: String,
    val message: String,
    val nodeId: String? = null,
    val port: String? = null,
)

/** Validates a [WorkflowDefinition] and returns all detected errors. */
interface WorkflowValidator {
    fun validate(workflow: WorkflowDefinition): List<ValidationError>
}
