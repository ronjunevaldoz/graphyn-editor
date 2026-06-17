package com.ronjunevaldoz.graphyn.plugins.math

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

object MathExecutors {
    private fun doubles(input: Map<String, WorkflowValue>): Pair<Double, Double> {
        val left = (input["left"] as? WorkflowValue.DoubleValue)?.value ?: 0.0
        val right = (input["right"] as? WorkflowValue.DoubleValue)?.value ?: 0.0
        return left to right
    }

    fun add(input: Map<String, WorkflowValue>): Map<String, WorkflowValue> {
        val (l, r) = doubles(input)
        return mapOf("result" to WorkflowValue.DoubleValue(l + r))
    }

    fun subtract(input: Map<String, WorkflowValue>): Map<String, WorkflowValue> {
        val (l, r) = doubles(input)
        return mapOf("result" to WorkflowValue.DoubleValue(l - r))
    }

    fun multiply(input: Map<String, WorkflowValue>): Map<String, WorkflowValue> {
        val (l, r) = doubles(input)
        return mapOf("result" to WorkflowValue.DoubleValue(l * r))
    }
}
