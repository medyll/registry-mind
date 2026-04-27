package com.registry.mind

import com.registry.mind.llm.ModelDownloadManager
import com.registry.mind.data.EnrichedEntry
import org.junit.Assert.*
import org.junit.Test

class LocalLlmTest {

    @Test
    fun `EnrichedEntry created with required fields`() {
        val entry = EnrichedEntry(
            id = "test-001",
            rawText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
            summary = "Lorem ipsum.",
            sourceApp = "com.example.app",
            tag = "work"
        )
        assertEquals("test-001", entry.id)
        assertEquals("work", entry.tag)
        assertNull(entry.exportedAt)
        assertTrue(entry.timestamp > 0)
    }

    @Test
    fun `EnrichedEntry exportedAt null by default`() {
        val entry = EnrichedEntry(
            id = "test-002",
            rawText = "text",
            summary = "summary",
            sourceApp = "app",
            tag = null
        )
        assertNull(entry.exportedAt)
    }

    @Test
    fun `ModelDownloadManager model filename constant`() {
        assertEquals("gemma-2b-it-q4.bin", ModelDownloadManager.MODEL_FILENAME)
    }
}
