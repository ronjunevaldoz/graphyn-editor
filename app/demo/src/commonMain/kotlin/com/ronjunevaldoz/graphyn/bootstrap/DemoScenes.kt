package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

// script.eval is JVM-only; type string is used here so this KMP module stays dependency-free.
private val scriptDemoWorkflow = WorkflowDefinition(
    id = "script-demo", name = "Script",
    nodes = listOf(
        NodeRef("format", "text.format",
            config = mapOf("template" to WorkflowValue.StringValue("Hello, {name}!"))),
        NodeRef("script", "script.eval",
            config = mapOf("code" to WorkflowValue.StringValue(
                "import java.time.LocalDate\n" +
                "\"[\${LocalDate.now()}] \$input\""
            ))),
        NodeRef("preview", "preview.view"),
    ),
    connections = listOf(
        ConnectionRef("format",  "result", "script",  "input"),
        ConnectionRef("script",  "result", "preview", "value"),
    ),
)

enum class DemoScene(val label: String, val workflow: WorkflowDefinition) {
    Styles("Styles",     styleNodesDemoWorkflow),
    ListOps("List Ops",  listOpsDemoWorkflow),
    Control("Control",   controlDemoWorkflow),
    Text("Text",         textDemoWorkflow),
    Types("Types",       typesDemoWorkflow),
    Io("I/O",            ioDemoWorkflow),
    Groups("Groups",     groupsDemoWorkflow),
    Subgraph("Subgraph", subgraphDemoWorkflow),
    Script("Script",     scriptDemoWorkflow),
}
