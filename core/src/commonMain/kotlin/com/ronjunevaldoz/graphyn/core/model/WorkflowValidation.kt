package com.ronjunevaldoz.graphyn.core.model

data class ValidationError(
    val code: String,
    val message: String,
    val nodeId: String? = null,
    val port: String? = null,
)

interface WorkflowValidator {
    fun validate(workflow: WorkflowDefinition): List<ValidationError>
}
