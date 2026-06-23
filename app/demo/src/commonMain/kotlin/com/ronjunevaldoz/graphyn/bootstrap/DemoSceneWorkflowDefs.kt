package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.plugins.stylenodes.StyleNodesSpecs

/** ComfyUI-aesthetic AI image generation pipeline. */
internal val aiPipelineWorkflow = WorkflowDefinition(
    id = "ai-pipeline-demo", name = "AI Pipeline",
    nodes = listOf(
        NodeRef("ckpt",    specCheckpointLoader.type),
        NodeRef("pos",     specClipEncode.type),
        NodeRef("neg",     specClipEncode.type),
        NodeRef("sampler", StyleNodesSpecs.kSampler.type),
        NodeRef("decode",  specVaeDecode.type),
        NodeRef("save",    specSaveImage.type),
    ),
    connections = listOf(
        ConnectionRef("ckpt", "clip",         "pos",     "clip"),
        ConnectionRef("ckpt", "clip",         "neg",     "clip"),
        ConnectionRef("ckpt", "model",        "sampler", "model"),
        ConnectionRef("ckpt", "vae",          "decode",  "vae"),
        ConnectionRef("pos",  "conditioning", "sampler", "positive"),
        ConnectionRef("neg",  "conditioning", "sampler", "negative"),
        ConnectionRef("sampler", "latent",    "decode",  "samples"),
        ConnectionRef("decode",  "image",     "save",    "image"),
    ),
)

/** Blender Geometry Nodes–aesthetic procedural geometry pipeline. */
internal val geometryPipelineWorkflow = WorkflowDefinition(
    id = "geometry-pipeline-demo", name = "Geometry Pipeline",
    nodes = listOf(
        NodeRef("prim",     specMeshPrimitive.type),
        NodeRef("subdivide",specSubdivideMesh.type),
        NodeRef("scatter",  StyleNodesSpecs.distributePoints.type),
        NodeRef("instance", specInstanceOnPoints.type),
        NodeRef("out",      specGeometryOutput.type),
    ),
    connections = listOf(
        ConnectionRef("prim",     "geometry", "subdivide", "geometry"),
        ConnectionRef("subdivide","geometry", "scatter",   "mesh"),
        ConnectionRef("scatter",  "points",   "instance",  "points"),
        ConnectionRef("instance", "geometry", "out",       "geometry"),
    ),
)

/** n8n-aesthetic automation pipeline: trigger → transform → filter → request → log. */
internal val automationPipelineWorkflow = WorkflowDefinition(
    id = "automation-pipeline-demo", name = "Automation Pipeline",
    nodes = listOf(
        NodeRef("trigger",  StyleNodesSpecs.webhook.type),
        NodeRef("set",      specSetField.type),
        NodeRef("filter",   specFilterIf.type),
        NodeRef("request",  specHttpRequestDemo.type),
        NodeRef("log",      specLogOutput.type),
    ),
    connections = listOf(
        ConnectionRef("trigger", "body",     "set",     "record"),
        ConnectionRef("set",     "record",   "filter",  "value"),
        ConnectionRef("filter",  "value",    "request", "body"),
        ConnectionRef("request", "response", "log",     "value"),
    ),
)
