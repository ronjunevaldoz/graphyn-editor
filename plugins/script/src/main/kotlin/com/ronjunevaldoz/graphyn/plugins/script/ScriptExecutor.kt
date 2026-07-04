package com.ronjunevaldoz.graphyn.plugins.script

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import javax.script.ScriptEngineManager

/**
 * Evaluates Kotlin scripts via JSR-223. Each call gets a fresh engine instance — reusing one engine
 * across multiple *different* scripts within a run was found to corrupt its internal compiler state:
 * after a handful of sequential eval() calls on the same engine, every subsequent script (regardless
 * of complexity) started failing with "Backend Internal error: Exception during psi2ir" even for
 * trivial one-liners. A fresh engine per call costs ~1-2s warm-up each time, but that's cheap next to
 * silently wrong results.
 *
 * `input` is unwrapped to its native Kotlin type before binding so scripts feel natural:
 * - `StringValue("hi")` → `"hi"` (String)
 * - `IntValue(3)` → `3` (Int)
 * - `BooleanValue(true)` → `true` (Boolean)
 * - `NullValue` → `null`
 *
 * The last expression of the script becomes `result` and is wrapped back automatically.
 */
internal object ScriptExecutor : NodeExecutor {

    override suspend fun execute(input: Map<String, WorkflowValue>): Map<String, WorkflowValue> {
        val code = (input["code"] as? WorkflowValue.StringValue)?.value
            ?: return outputs(WorkflowValue.NullValue, "No code provided")
        val inputVal = input["input"] ?: WorkflowValue.NullValue
        return try {
            val engine = ScriptEngineManager().getEngineByExtension("kts")
                ?: error("Kotlin scripting engine not found on classpath")
            engine.put("input", inputVal.unwrap())
            val raw = engine.eval(code)
            outputs(raw.toWorkflowValue(), "")
        } catch (e: Exception) {
            outputs(WorkflowValue.NullValue, e.message ?: "Script error")
        }
    }

    private fun outputs(result: WorkflowValue, error: String) = mapOf(
        "result" to result,
        "error"  to WorkflowValue.StringValue(error),
    )

    private fun WorkflowValue.unwrap(): Any? = when (this) {
        is WorkflowValue.StringValue  -> value
        is WorkflowValue.IntValue     -> value
        is WorkflowValue.DoubleValue  -> value
        is WorkflowValue.BooleanValue -> value
        is WorkflowValue.ListValue    -> items.map { it.unwrap() }
        is WorkflowValue.RecordValue  -> fields.mapValues { it.value.unwrap() }
        else                          -> null
    }

    private fun Any?.toWorkflowValue(): WorkflowValue = when (this) {
        is WorkflowValue -> this
        is Map<*, *> -> WorkflowValue.RecordValue(
            this.entries.associate { (key, value) -> key.toString() to value.toWorkflowValue() },
        )
        is List<*> -> WorkflowValue.ListValue(this.map { it.toWorkflowValue() })
        is String -> WorkflowValue.StringValue(this)
        is Int -> WorkflowValue.IntValue(this)
        is Long -> WorkflowValue.IntValue(this.toInt())
        is Double -> WorkflowValue.DoubleValue(this)
        is Float -> WorkflowValue.DoubleValue(this.toDouble())
        is Boolean -> WorkflowValue.BooleanValue(this)
        null -> WorkflowValue.NullValue
        else -> WorkflowValue.StringValue(this.toString())
    }
}
