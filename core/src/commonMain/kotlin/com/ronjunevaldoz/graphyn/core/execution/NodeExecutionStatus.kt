package com.ronjunevaldoz.graphyn.core.execution

/** Lifecycle phase of a single node during a workflow run. */
enum class NodeExecutionStatus {
    /** Node has not been executed in the current run. */
    Idle,
    /** Execution is in progress. */
    Running,
    /** Execution completed without error. */
    Success,
    /** Execution threw an exception or returned an error result. */
    Error,
}
