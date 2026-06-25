package com.ronjunevaldoz.graphyn.plugins.script

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

internal const val CATEGORY_SCRIPT = "script"

/**
 * Node spec for the Kotlin script executor.
 *
 * `code` is a config-only field edited inline on the card (not a wire port).
 * Inside the script, `input` is bound to whatever arrives on the `input` port as a native
 * Kotlin type (`String`, `Int`, `Boolean`, `List`, `Map`, or `null`).
 * The last expression in the script becomes `result`.
 *
 * **String template escaping in workflow definitions**: When defining script code in a
 * triple-quoted Kotlin string (e.g., in `WorkflowDefinition`), use `$$variable` instead of
 * `$variable` to prevent the outer Kotlin compiler from interpolating the string. The script
 * evaluator will then see the literal `$variable` and interpolate it at runtime.
 *
 * Example script:
 * ```kotlin
 * import java.time.LocalDate
 * "Hello from ${LocalDate.now()} — input was: $input"
 * ```
 *
 * Example in workflow definition (triple-quoted):
 * ```kotlin
 * WorkflowValue.StringValue(
 *   """
 *   val formatted = String.format("%.1f", input as Double)
 *   "Result: $$formatted"
 *   """.trimIndent()
 * )
 * ```
 */
internal val specScriptEval = NodeSpec(
    type = "script.eval",
    label = "Script",
    description = "Evaluates a Kotlin .kts script. 'input' binding holds the connected value.",
    category = CATEGORY_SCRIPT,
    inputs = listOf(
        PortSpec("input", WorkflowType.OpaqueType, required = false),
    ),
    outputs = listOf(
        PortSpec("result", WorkflowType.OpaqueType),
        PortSpec("error",  WorkflowType.StringType),
    ),
    defaultValues = mapOf(
        "code" to WorkflowValue.StringValue("// 'input' is available as a native Kotlin value\ninput"),
    ),
)
