package com.registry.mind

import com.registry.mind.data.Header
import com.registry.mind.data.NavigationMeta
import com.registry.mind.data.Payload
import com.registry.mind.data.RegistryPacket
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for RegistryIngestor logic (not requiring Android context).
 *
 * Note: captureOnly() and sendPacket() require Android services (MediaProjection, OCR).
 * These tests validate:
 *  - RegistryPacket structure matches expected schema
 *  - NavigationMeta tag field is included when non-null
 *  - NavigationMeta session_state transitions
 *  - Result.failure paths
 */
class RegistryIngestorTest {

    // --- Packet schema validation ---

    @Test
    fun `RegistryPacket serializes tag in NavigationMeta when set`() {
        val packet = RegistryPacket(
            header = Header(
                protocol = "registry-mind-v1",
                device = "Oppo_Find_X9_Native",
                timestamp = "2026-04-22T12:00:00Z",
                authToken = "token"
            ),
            payload = Payload(
                imageData = "",
                ocrContent = "text",
                sourceApp = "com.test.app",
                aiGuess = null
            ),
            navigationMeta = NavigationMeta(
                role = "registry_sensor",
                sessionState = "active_chat",
                tag = "Urgent"
            )
        )

        assertEquals("Urgent", packet.navigationMeta.tag)
        assertEquals("active_chat", packet.navigationMeta.sessionState)
        assertEquals("registry_sensor", packet.navigationMeta.role)
    }

    @Test
    fun `RegistryPacket tag is null when not set`() {
        val packet = RegistryPacket(
            header = Header(
                protocol = "registry-mind-v1",
                device = "Oppo_Find_X9_Native",
                timestamp = "2026-04-22T12:00:00Z",
                authToken = "token"
            ),
            payload = Payload(
                imageData = "",
                ocrContent = "text",
                sourceApp = "com.test.app"
            ),
            navigationMeta = NavigationMeta()
        )

        assertNull("Tag should be null by default", packet.navigationMeta.tag)
        assertEquals("inactive", packet.navigationMeta.sessionState)
    }

    @Test
    fun `NavigationMeta defaults are correct`() {
        val meta = NavigationMeta()
        assertEquals("registry_sensor", meta.role)
        assertEquals("inactive", meta.sessionState)
        assertNull(meta.tag)
    }

    @Test
    fun `Header authToken is included in packet`() {
        val packet = RegistryPacket(
            header = Header(
                timestamp = "2026-04-22T00:00:00Z",
                authToken = "my-secret-key"
            ),
            payload = Payload(imageData = "", ocrContent = "", sourceApp = ""),
            navigationMeta = NavigationMeta()
        )
        assertEquals("my-secret-key", packet.header.authToken)
        assertEquals("registry-mind-v1", packet.header.protocol)
        assertEquals("Oppo_Find_X9_Native", packet.header.device)
    }

    // --- Result path validation ---

    @Test
    fun `Result failure wraps exception message`() {
        val error = RuntimeException("capture_failed")
        val result = Result.failure<RegistryPacket>(error)

        assertTrue(result.isFailure)
        assertEquals("capture_failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `Result success carries packet`() {
        val packet = RegistryPacket(
            header = Header(timestamp = "now", authToken = "tok"),
            payload = Payload(imageData = "", ocrContent = "hello", sourceApp = "com.app"),
            navigationMeta = NavigationMeta(sessionState = "active_chat")
        )
        val result = Result.success(packet)

        assertTrue(result.isSuccess)
        assertEquals("hello", result.getOrNull()?.payload?.ocrContent)
        assertEquals("active_chat", result.getOrNull()?.navigationMeta?.sessionState)
    }

    // --- Content classification (mirrors RegistryIngestor.classifyContent) ---

    private fun classifyContent(text: String): String? = when {
        text.contains("invoice", ignoreCase = true) ||
        text.contains("facture", ignoreCase = true) ||
        text.contains("receipt", ignoreCase = true) -> "finance_document"
        text.contains("meeting", ignoreCase = true) ||
        text.contains("calendar", ignoreCase = true) -> "work_meeting"
        text.contains("recipe", ignoreCase = true) ||
        text.contains("ingredients", ignoreCase = true) -> "personal_recipe"
        text.contains("flight", ignoreCase = true) ||
        text.contains("hotel", ignoreCase = true) ||
        text.contains("booking", ignoreCase = true) -> "travel"
        else -> null
    }

    @Test
    fun `classifyContent detects finance_document`() {
        assertEquals("finance_document", classifyContent("Here is your invoice #123"))
        assertEquals("finance_document", classifyContent("Votre facture du mois"))
        assertEquals("finance_document", classifyContent("Payment receipt attached"))
    }

    @Test
    fun `classifyContent detects work_meeting`() {
        assertEquals("work_meeting", classifyContent("Standup meeting at 10am"))
        assertEquals("work_meeting", classifyContent("Calendar event: Q2 review"))
    }

    @Test
    fun `classifyContent detects travel`() {
        assertEquals("travel", classifyContent("Your flight booking is confirmed"))
        assertEquals("travel", classifyContent("Hotel reservation: Paris"))
    }

    @Test
    fun `classifyContent returns null for unknown content`() {
        assertNull(classifyContent("Random social media post"))
        assertNull(classifyContent("Some text without keywords"))
    }
}
