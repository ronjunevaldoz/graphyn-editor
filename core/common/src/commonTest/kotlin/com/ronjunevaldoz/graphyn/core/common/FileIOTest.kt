package com.ronjunevaldoz.graphyn.core.common

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FileIOTest {

    @Test
    fun resolvePathJoinsPathsCorrectly() = runTest {
        val base = "/usr/local"
        val relative = "bin/ffmpeg"
        // We normalize '/' for cross-platform test predictability in commonTest
        val expected = "/usr/kotlin/local/bin/ffmpeg".replace("kotlin", "") // Just a trick to clean up my thought process error
        // Let me rewrite this correctly as I am rambling to myself.
        val realExpected = "/usr/local/bin/ffmpeg"
        val actual = FileIO.resolvePath(base, relative).replace('\\', '/')
        
        assertEquals(realExpected, actual)
    }

    @Test
    fun resolvePathHandlesAbsoluteRelativePathAsAbsolutePath() = runTest {
        val base = "/usr/local"
        val relative = "/etc/passwd"
        val expected = "/etc/passwd"
        val actual = FileIO.resolvePath(base, relative).replace('\\', '/')
        
        assertEquals(expected, actual)
    }

    @Test
    fun resolvePathHandlesEmptyBaseDir() = runTest {
        val base = ""
        val relative = "configs/settings.json"
        val expected = "configs/settings.json"
        val actual = FileIO.resolvePath(base, relative).replace('\\', '/')
        
        assertEquals(expected, actual)
    }

    @Test
    fun resolvePathHandlesEmptyRelativePath() = runTest {
        val base = "/home/user"
        val relative = ""
        val expected = "/home/user"
        val actual = FileIO.resolvePath(base, relative).replace('\\', '/')
        
        assertEquals(expected, actual)
    }

    @Test
    fun resolvePathCleansUpRedundantSlashes() = runTest {
        val base = "/usr/local/"
        val relative = "//bin//ffmpeg"
        val target = "/usr/local/bin/ffmpeg"
        val actual = FileIO.resolvePath(base, relative).replace('\\', '/')

        assertEquals(target, actual)
    }

}
