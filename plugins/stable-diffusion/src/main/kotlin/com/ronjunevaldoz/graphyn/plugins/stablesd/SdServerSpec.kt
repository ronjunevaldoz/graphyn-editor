package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.NullableType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.OpaqueType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.StringType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Node spec for `sd.server` — a remote `server-sd` deployment to send generation requests to.
 *
 * Wire this into a generation node's `server` input to pin that node to a specific deployment
 * (e.g. a Modal-hosted `server-sd` instance), independent of the app-wide server URL/API key set
 * in Settings. Multiple `sd.server` nodes can coexist in one workflow so different branches can
 * target different servers. Leave the port unwired to keep using the app-wide default.
 *
 * Only the HTTP backend honors this token; a local CLI-process backend ignores it.
 */
object SdServerSpec {
    val server = NodeSpec(
        type = "sd.server",
        label = "SD Server",
        description = "Remote server-sd URL + API key. Wire into a generation node's `server` port to target that deployment.",
        category = CATEGORY_SD_CFG,
        inputs = listOf(
            PortSpec("base_url", StringType, portColor = COLOR_SERVER,
                description = "Base URL of the server-sd deployment, e.g. https://my-app.modal.run. Blank = app-wide default."),
            PortSpec("api_key", NullableType(StringType), portColor = COLOR_SERVER,
                description = "Bearer token sent as Authorization header. Null = no auth."),
        ),
        outputs = listOf(
            PortSpec("server", OpaqueType, portColor = COLOR_SERVER,
                description = "Opaque server connection token passed to generation nodes."),
        ),
        defaultValues = mapOf(
            "base_url" to WorkflowValue.StringValue(""),
        ),
    )
}
