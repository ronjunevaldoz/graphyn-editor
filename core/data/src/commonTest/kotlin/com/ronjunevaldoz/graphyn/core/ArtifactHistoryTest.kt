package com.ronjunevaldoz.graphyn.core

import com.ronjunevaldoz.graphyn.core.store.ArtifactKind
import com.ronjunevaldoz.graphyn.core.store.ArtifactRecord
import com.ronjunevaldoz.graphyn.core.store.InMemoryArtifactHistory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ArtifactHistoryTest {

    private fun rec(id: String, at: Long) = ArtifactRecord(
        id = id, kind = ArtifactKind.Image, path = "/out/$id.png", createdAt = at,
    )

    @Test
    fun listsNewestFirst() = runTest {
        val history = InMemoryArtifactHistory()
        history.record(rec("a", 1))
        history.record(rec("b", 2))
        history.record(rec("c", 3))

        assertEquals(listOf("c", "b", "a"), history.list().map { it.id })
    }

    @Test
    fun limitCapsResults() = runTest {
        val history = InMemoryArtifactHistory()
        repeat(5) { history.record(rec("r$it", it.toLong())) }

        assertEquals(listOf("r4", "r3"), history.list(limit = 2).map { it.id })
    }

    @Test
    fun clearEmptiesHistory() = runTest {
        val history = InMemoryArtifactHistory()
        history.record(rec("a", 1))
        history.clear()
        assertEquals(emptyList(), history.list())
    }
}
