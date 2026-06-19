package com.ronjunevaldoz.graphyn.plugins.script

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import javax.script.ScriptEngineManager

/**
 * Evaluates Kotlin scripts via JSR-223. Engine is lazy — first call pays ~1-2 s warm-up.
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

    private val engine by lazy {
        ScriptEngineManager().getEngineByExtension("kts")
            ?: error("Kotlin scripting engine not found on classpath")
    }

    override suspend fun execute(input: Map<String, WorkflowValue>): Map<String, WorkflowValue> {
        val code = (input["code"] as? WorkflowValue.StringValue)?.value
            ?: return outputs(WorkflowValue.NullValue, "No code provided")
        val inputVal = input["input"] ?: WorkflowValue.NullValue
        return try {
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
        is String        -> WorkflowValue.StringValue(this)
        is Int           -> WorkflowValue.IntValue(this)
        is Long          -> WorkflowValue.IntValue(this.toInt())
        is Double        -> WorkflowValue.DoubleValue(this)
        is Float         -> WorkflowValue.DoubleValue(this.toDouble())
        is Boolean       -> WorkflowValue.BooleanValue(this)
        null             -> WorkflowValue.NullValue
        else             -> WorkflowValue.StringValue(this.toString())
    }
}
