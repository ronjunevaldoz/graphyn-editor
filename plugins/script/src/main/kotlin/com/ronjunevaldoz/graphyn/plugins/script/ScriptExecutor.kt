package com.ronjunevaldoz.graphyn.plugins.script

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import javax.script.ScriptEngineManager

/**
 * Evaluates Kotlin scripts via JSR-223. The engine is lazy so the first execution pays the
 * warm-up cost (~1-2 s); subsequent runs reuse the same engine instance.
 *
 * Script context: the `input` binding is a [WorkflowValue] available as a top-level variable.
 * The last expression of the script becomes `result`.
 */
internal object ScriptExecutor : NodeExecutor {

    private val engine by lazy {
        ScriptEngineManager().getEngineByExtension("kts")
            ?: error("Kotlin scripting engine not found on classpath")
    }

    override suspend fun execute(input: Map<String, WorkflowValue>): Map<String, WorkflowValue> {
        val code = (input["code"] as? WorkflowValue.StringValue)?.value
            ?: return outputs(result = WorkflowValue.NullValue, error = "No code provided")

        val inputValue = input["input"] ?: WorkflowValue.NullValue
        return try {
            engine.put("input", inputValue)
            val raw = engine.eval(code)
            outputs(result = raw.toWorkflowValue(), error = "")
        } catch (e: Exception) {
            outputs(result = WorkflowValue.NullValue, error = e.message ?: "Script error")
        }
    }

    private fun outputs(result: WorkflowValue, error: String) = mapOf(
        "result" to result,
        "error"  to WorkflowValue.StringValue(error),
    )

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
