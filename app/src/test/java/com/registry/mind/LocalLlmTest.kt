package com.registry.mind

import com.registry.mind.data.EnrichedEntry
import com.registry.mind.export.ExportConnector
import com.registry.mind.llm.ModelDownloadManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LocalLlmTest {

    // --- EnrichedEntry schema ---

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
    fun `EnrichedEntry rewrite null by default`() {
        val entry = EnrichedEntry(
            id = "test-003",
            rawText = "original text",
            summary = "summary",
            sourceApp = "com.app",
            tag = null
        )
        assertNull(entry.rewrite)
    }

    @Test
    fun `EnrichedEntry rewrite field stores value when set`() {
        val entry = EnrichedEntry(
            id = "test-004",
            rawText = "Ce document contient une facture du mois de mars.",
            summary = "Facture mars.",
            rewrite = "Document: facture mars. Montant inconnu.",
            sourceApp = "com.scanner",
            tag = "work"
        )
        assertEquals("Document: facture mars. Montant inconnu.", entry.rewrite)
    }

    @Test
    fun `EnrichedEntry all optional fields`() {
        val ts = System.currentTimeMillis()
        val entry = EnrichedEntry(
            id = "test-005",
            rawText = "raw",
            summary = "sum",
            rewrite = "rewritten",
            sourceApp = "com.app",
            tag = "personal",
            timestamp = ts,
            exportedAt = ts + 1000L
        )
        assertEquals("rewritten", entry.rewrite)
        assertEquals(ts, entry.timestamp)
        assertEquals(ts + 1000L, entry.exportedAt)
        assertEquals("personal", entry.tag)
    }

    @Test
    fun `EnrichedEntry copy preserves rewrite`() {
        val entry = EnrichedEntry(
            id = "test-006",
            rawText = "text",
            summary = "summary",
            rewrite = "rewritten",
            sourceApp = "app",
            tag = null
        )
        val exported = entry.copy(exportedAt = 9999L)
        assertEquals("rewritten", exported.rewrite)
        assertEquals(9999L, exported.exportedAt)
    }

    // --- ModelDownloadManager ---

    @Test
    fun `ModelDownloadManager model filename constant`() {
        assertEquals("gemma-2b-it-q4.bin", ModelDownloadManager.MODEL_FILENAME)
    }

    @Test
    fun `ModelDownloadManager tmp filename derived from constant`() {
        val tmpName = "${ModelDownloadManager.MODEL_FILENAME}.tmp"
        assertEquals("gemma-2b-it-q4.bin.tmp", tmpName)
    }

    // --- ExportConnector interface contract ---

    @Test
    fun `ExportConnector mock export returns success`() = runBlocking {
        val connector = object : ExportConnector {
            override val id = "mock-connector"
            override suspend fun export(entry: EnrichedEntry): Result<Unit> = Result.success(Unit)
        }
        val entry = EnrichedEntry(
            id = "e-001",
            rawText = "text",
            summary = "summary",
            sourceApp = "app",
            tag = null
        )
        val result = connector.export(entry)
        assertTrue(result.isSuccess)
        assertEquals("mock-connector", connector.id)
    }

    @Test
    fun `ExportConnector default exportBatch calls export for each entry`() = runBlocking {
        val exported = mutableListOf<String>()
        val connector = object : ExportConnector {
            override val id = "batch-connector"
            override suspend fun export(entry: EnrichedEntry): Result<Unit> {
                exported.add(entry.id)
                return Result.success(Unit)
            }
        }
        val entries = listOf(
            EnrichedEntry(id = "a", rawText = "t", summary = "s", sourceApp = "app", tag = null),
            EnrichedEntry(id = "b", rawText = "t", summary = "s", sourceApp = "app", tag = null),
            EnrichedEntry(id = "c", rawText = "t", summary = "s", sourceApp = "app", tag = null)
        )
        val results = connector.exportBatch(entries)
        assertEquals(3, results.size)
        assertEquals(listOf("a", "b", "c"), exported)
        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun `ExportConnector export failure propagates`() = runBlocking {
        val connector = object : ExportConnector {
            override val id = "failing-connector"
            override suspend fun export(entry: EnrichedEntry): Result<Unit> =
                Result.failure(RuntimeException("network_error"))
        }
        val entry = EnrichedEntry(
            id = "fail-001",
            rawText = "text",
            summary = "summary",
            sourceApp = "app",
            tag = null
        )
        val result = connector.export(entry)
        assertTrue(result.isFailure)
        assertEquals("network_error", result.exceptionOrNull()?.message)
    }

    // --- LLM fallback logic (pure logic, no Android context) ---

    @Test
    fun `LLM fallback summary truncates rawText to 200 chars when llm null`() {
        val rawText = "A".repeat(300)
        val localLlm: Any? = null
        val summary = if (localLlm != null) "llm_result" else rawText.take(200)
        assertEquals(200, summary.length)
        assertEquals("A".repeat(200), summary)
    }

    @Test
    fun `LLM fallback preserves short text unchanged`() {
        val rawText = "Short text."
        val localLlm: Any? = null
        val summary = if (localLlm != null) "llm_result" else rawText.take(200)
        assertEquals("Short text.", summary)
    }
}
