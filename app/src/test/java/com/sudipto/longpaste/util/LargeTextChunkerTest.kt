package com.sudipto.longpaste.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LargeTextChunkerTest {
    @Test
    fun preservesOneMegabyteText() {
        val source = "abcd".repeat(262_144)
        val chunks = LargeTextChunker.chunks(source, 16_384).map { it.toString() }.toList()
        val reconstructed = chunks.joinToString("")
        assertEquals(1_048_576, source.length)
        assertEquals(source, reconstructed)
        assertEquals(64, chunks.size)
    }

    @Test
    fun handlesUnicodeWithoutLoss() {
        val source = "বাংলা 🚀 café 你好".repeat(20_000)
        val reconstructed = LargeTextChunker.chunks(source, 1_000).joinToString("")
        assertEquals(source, reconstructed)
    }

    @Test
    fun countReturnsCeiling() {
        assertEquals(0, LargeTextChunker.count("", 10))
        assertEquals(1, LargeTextChunker.count("1234567890", 10))
        assertEquals(2, LargeTextChunker.count("12345678901", 10))
    }

    @Test
    fun defaultChunkSizeIsReasonable() {
        assertTrue(LargeTextChunker.DEFAULT_CHUNK_SIZE in 8_192..65_536)
    }
}
