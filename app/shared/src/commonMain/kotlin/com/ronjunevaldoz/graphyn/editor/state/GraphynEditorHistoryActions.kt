package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.unit.IntOffset
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition

internal fun GraphynEditorState.moveSelectedNodes(delta: IntOffset) {
    effectiveSelectedNodeIds.forEach { nodeId -> layout.moveNode(nodeId, delta) }
}

internal fun GraphynEditorState.toggleNodeSelection(nodeId: String) {
    selectedConnection = null
    selectedNodeId = null
    selectedNodeIds = if (nodeId in selectedNodeIds) selectedNodeIds - nodeId else selectedNodeIds + nodeId
}

internal fun GraphynEditorState.selectAllNodes() {
    selectedNodeId = null
    selectedConnection = null
    selectedNodeIds = workflow?.nodes?.mapTo(mutableSetOf()) { it.id } ?: emptySet()
}

internal fun GraphynEditorState.copySelection() {
    val ids = effectiveSelectedNodeIds
    if (ids.isEmpty()) return
    val nodes = workflow?.nodes?.filter { it.id in ids } ?: return
    clipboard.copy(nodes.map { it to (layout.nodePositionsByNodeId[it.id] ?: IntOffset.Zero) })
    log.push("Copied ${nodes.size} node(s)")
}

internal fun GraphynEditorState.pasteNodes() {
    if (!clipboard.hasContent) return
    val current = workflow ?: WorkflowDefinition("workflow", "Untitled Workflow", emptyList(), emptyList())
    val existing = current.nodes.mapTo(mutableSetOf()) { it.id }
    val pastedIds = mutableSetOf<String>()
    val newNodes = clipboard.clipboard.map { (node, pos) ->
        val newId = uniqueId(node.id, existing)
        existing += newId
        pastedIds += newId
        val offset = IntOffset(pos.x + 40, pos.y + 40)
        layout.setNodePosition(newId, offset)
        newId to NodeRef(newId, node.type, node.config)
    }
    workflow = current.copy(nodes = current.nodes + newNodes.map { it.second })
    selectedNodeIds = pastedIds
    selectedNodeId = null
    log.push("Pasted ${newNodes.size} node(s)")
}

internal fun GraphynEditorState.duplicateSelection() {
    copySelection()
    pasteNodes()
}

private fun uniqueId(base: String, existing: Set<String>): String {
    var id = "${base}-copy"
    var n = 1
    while (id in existing) { id = "${base}-copy-${n++}" }
    return id
}
