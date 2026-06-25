package com.ronjunevaldoz.graphyn.core

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.store.InMemoryWorkflowStore
import com.ronjunevaldoz.graphyn.core.store.WorkflowDiffComputer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkflowStoreTest {

    private fun workflow(
        id: String = "wf-1",
        nodes: List<NodeRef> = emptyList(),
        connections: List<ConnectionRef> = emptyList(),
    ) = WorkflowDefinition(id, "Test $id", nodes, connections)

    private fun node(id: String, type: String = "test.node") = NodeRef(id, type)

    @Test
    fun saveAndLoad() = runTest {
        val store = InMemoryWorkflowStore()
        val wf = workflow(nodes = listOf(node("a"), node("b")))
        store.save(wf)
        assertEquals(wf, store.load(wf.id))
    }

    @Test
    fun listReturnsMeta() = runTest {
        val store = InMemoryWorkflowStore()
        store.save(workflow("w1"))
        store.save(workflow("w2"))
        val metas = store.list()
        assertEquals(2, metas.size)
        assertTrue(metas.any { it.id == "w1" } && metas.any { it.id == "w2" })
    }

    @Test
    fun historyTracksVersionsAndDiff() = runTest {
        val store = InMemoryWorkflowStore()
        val v1 = workflow(nodes = listOf(node("a")))
        val v2 = workflow(nodes = listOf(node("a"), node("b")))

        store.save(v1)
        store.save(v2)

        val versions = store.history(v1.id)
        assertEquals(2, versions.size)
        // Newest first
        assertEquals(v2, versions[0].snapshot)
        // First version has no diff (no predecessor)
        assertNull(versions[1].diff)
        // Second version diff shows node "b" added
        val diff = versions[0].diff
        assertNotNull(diff)
        assertEquals(1, diff.nodesAdded.size)
        assertEquals("b", diff.nodesAdded.first().id)
        assertTrue(diff.nodesRemoved.isEmpty())
    }

    @Test
    fun deleteRemovesWorkflow() = runTest {
        val store = InMemoryWorkflowStore()
        store.save(workflow("x"))
        store.delete("x")
        assertNull(store.load("x"))
        assertTrue(store.list().isEmpty())
    }

    @Test
    fun markExecutedUpdatesTimestamp() = runTest {
        val store = InMemoryWorkflowStore()
        store.save(workflow("run-me"))
        assertNull(store.list().first().lastExecutedAt)
        store.markExecuted("run-me")
        assertNotNull(store.list().first().lastExecutedAt)
    }

    @Test
    fun diffComputerDetectsConnections() {
        val a = node("a"); val b = node("b")
        val conn = ConnectionRef("a", "out", "b", "in")
        val before = workflow(nodes = listOf(a, b))
        val after = workflow(nodes = listOf(a, b), connections = listOf(conn))
        val diff = WorkflowDiffComputer.compute(before, after)
        assertEquals(listOf(conn), diff.connectionsAdded)
        assertTrue(diff.connectionsRemoved.isEmpty())
        assertTrue(diff.isEmpty.not())
    }
}
