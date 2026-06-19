package com.ronjunevaldoz.graphyn.plugins.script

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

internal const val CATEGORY_SCRIPT = "script"

/**
 * Node spec for the Kotlin script executor.
 *
 * The `code` port holds the script source. Inside the script, `input` is bound to
 * whatever value arrives on the `input` port (or [WorkflowValue.NullValue] if unwired).
 * The last expression in the script becomes `result`.
 *
 * Example script:
 * ```kotlin
 * import java.time.LocalDate
 * "Hello from ${LocalDate.now()} — input was: $input"
 * ```
 */
internal val specScriptEval = NodeSpec(
    type = "script.eval",
    label = "Script",
    description = "Evaluates a Kotlin .kts script. 'input' binding holds the connected value.",
    category = CATEGORY_SCRIPT,
    inputs = listOf(
        PortSpec("code",  WorkflowType.StringType,  required = false),
        PortSpec("input", WorkflowType.OpaqueType,  required = false),
    ),
    outputs = listOf(
        PortSpec("result", WorkflowType.OpaqueType),
        PortSpec("error",  WorkflowType.StringType),
    ),
    defaultValues = mapOf(
        "code" to WorkflowValue.StringValue("// 'input' is available as a WorkflowValue\ninput"),
    ),
)
