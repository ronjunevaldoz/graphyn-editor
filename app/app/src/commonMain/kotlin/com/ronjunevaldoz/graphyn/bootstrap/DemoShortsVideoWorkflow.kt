package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.GRAPHYN_SUBGRAPH_TYPE
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

private fun s(value: String) = WorkflowValue.StringValue(value)

internal val videoShortsWorkflow = WorkflowDefinition(
    id = "ai-shorts-video",
    name = "AI Shorts (Video Motion)",
    nodePositions = shortsNodePositions(),
    nodes = buildList {
        add(guideNote("AI Shorts\n\nBuild an 8-beat vertical short from Ollama, reusable scene subgraphs, stitching, captions, and encode.", height = 300))
        add(NodeRef("outlineSource", GRAPHYN_SUBGRAPH_TYPE, subgraph = shortsOutlineSubgraph()))
        add(NodeRef("outline", "json.parse"))
        add(NodeRef("scenes", "json.path", config = mapOf("path" to s("scenes"))))
        add(NodeRef("captions", "script.eval", config = mapOf("code" to s(SHORTS_CAPTIONS_SCRIPT))))
        repeat(SHORTS_SCENE_COUNT) { index ->
            val scene = index + 1
            add(NodeRef("scene$scene", SHORTS_SCENE_SUBGRAPH_NODE_TYPE, subgraph = videoSceneSubgraph(scene)))
        }
        add(NodeRef("batch1", SHORTS_BATCH_SUBGRAPH_NODE_TYPE, subgraph = stitchBatchSubgraph(1)))
        add(NodeRef("batch2", SHORTS_BATCH_SUBGRAPH_NODE_TYPE, subgraph = stitchBatchSubgraph(2)))
        add(NodeRef("finalBatch", SHORTS_BATCH_SUBGRAPH_NODE_TYPE, subgraph = stitchBatchSubgraph(3)))
        add(shortsCaptionStyleNode())
        add(NodeRef("captionOverlay", "media.caption_overlay"))
        add(NodeRef("encode", "media.video_encode", config = mapOf("output_path" to s("ai-shorts-video.mp4"), "bitrate" to s("high"), "codec" to s("h264"))))
        add(NodeRef("output", "media.file_output"))
    },
    connections = buildList {
        add(ConnectionRef("outlineSource", "value", "outline", "text"))
        add(ConnectionRef("outline", "value", "scenes", "value"))
        add(ConnectionRef("scenes", "result", "captions", "input"))
        repeat(SHORTS_SCENE_COUNT) { index ->
            val scene = index + 1
            add(ConnectionRef("scenes", "result", "scene$scene", "prompt"))
        }
        repeat(SHORTS_SCENE_COUNT - 1) { index ->
            val scene = index + 1
            add(ConnectionRef("scene$scene", "video", "scene${scene + 1}", "gate"))
        }
        add(ConnectionRef("scene1", "video", "batch1", "video1"))
        add(ConnectionRef("scene2", "video", "batch1", "video2"))
        add(ConnectionRef("scene3", "video", "batch1", "video3"))
        add(ConnectionRef("scene4", "video", "batch1", "video4"))
        add(ConnectionRef("scene5", "video", "batch2", "video1"))
        add(ConnectionRef("scene6", "video", "batch2", "video2"))
        add(ConnectionRef("scene7", "video", "batch2", "video3"))
        add(ConnectionRef("scene8", "video", "batch2", "video4"))
        add(ConnectionRef("batch1", "video", "finalBatch", "video1"))
        add(ConnectionRef("batch2", "video", "finalBatch", "video2"))
        add(ConnectionRef("finalBatch", "video", "captionOverlay", "video"))
        add(ConnectionRef("captions", "result", "captionOverlay", "captions"))
        add(ConnectionRef("captionStyle", "style_config", "captionOverlay", "style_config"))
        add(ConnectionRef("captionOverlay", "video", "encode", "video"))
        add(ConnectionRef("encode", "file_path", "output", "file_path"))
    },
)
