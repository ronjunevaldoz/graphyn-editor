package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.plugins.stylenodes.StyleNodesSpecs

// Demo workflow showing all three card styles on one canvas:
//   Webhook (CircleCard) → KSampler (DarkHeaderCard) → DistributePoints (FieldCard)
// Connection uses OpaqueType on both ends (kSampler.latent → distributePoints.mesh).
val demoWorkflow = WorkflowDefinition(
    id = "demo",
    name = "Style Demo",
    nodes = listOf(
        NodeRef("webhook",  StyleNodesSpecs.webhook.type),
        NodeRef("sampler",  StyleNodesSpecs.kSampler.type),
        NodeRef("scatter",  StyleNodesSpecs.distributePoints.type),
    ),
    connections = listOf(
        ConnectionRef("sampler", "latent", "scatter", "mesh"),
    ),
)
