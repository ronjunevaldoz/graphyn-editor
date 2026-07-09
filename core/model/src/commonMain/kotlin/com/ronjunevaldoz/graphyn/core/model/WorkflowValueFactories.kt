package com.ronjunevaldoz.graphyn.core.model

/**
 * Small factory helpers for building [WorkflowValue] instances without repeating the nested
 * `WorkflowValue.StringValue(...)` / `WorkflowValue.IntValue(...)` syntax at every call site.
 *
 * These stay in `core:model` so app and plugin modules can share them without inventing their own
 * local aliases.
 */
public fun stringValue(value: String): WorkflowValue.StringValue = WorkflowValue.StringValue(value)

/** Builds an integer [WorkflowValue] without repeating the wrapper type name. */
public fun intValue(value: Int): WorkflowValue.IntValue = WorkflowValue.IntValue(value)

/** Builds a double [WorkflowValue] without repeating the wrapper type name. */
public fun doubleValue(value: Double): WorkflowValue.DoubleValue = WorkflowValue.DoubleValue(value)

/** Builds a boolean [WorkflowValue] without repeating the wrapper type name. */
public fun booleanValue(value: Boolean): WorkflowValue.BooleanValue = WorkflowValue.BooleanValue(value)
