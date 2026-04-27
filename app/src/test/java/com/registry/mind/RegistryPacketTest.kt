package com.registry.mind

import com.google.gson.Gson
import com.registry.mind.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.Instant

class RegistryPacketTest {
    
    private val gson = Gson()
    
    @Test
    fun `packet serializes to correct JSON structure`() {
        val packet = RegistryPacket(
            header = Header(
                protocol = "registry-mind-v1",
                device = "Oppo_Find_X9_Native",
                timestamp = Instant.now().toString(),
                authToken = "test-key-123"
            ),
            payload = Payload(
                imageData = "base64_test_data",
                ocrContent = "Extracted text from screen",
                sourceApp = "com.android.chrome",
                aiGuess = "finance_facture"
            ),
            navigationMeta = NavigationMeta(
                role = "registry_sensor",
                sessionState = "active_chat"
            )
        )
        
        val json = gson.toJson(packet)
        assertNotNull(json)
        assertEquals(true, json.contains("\"protocol\":\"registry-mind-v1\""))
        assertEquals(true, json.contains("\"image_data\":\"base64_test_data\""))
        assertEquals(true, json.contains("\"ocr_content\":\"Extracted text from screen\""))
    }
    
    @Test
    fun `packet deserializes from JSON correctly`() {
        val json = """
        {
            "header": {
                "protocol": "registry-mind-v1",
                "device": "Oppo_Find_X9_Native",
                "timestamp": "2026-04-18T15:00:00Z",
                "auth_token": "test-key"
            },
            "payload": {
                "image_data": "base64_blob",
                "ocr_content": "Test OCR",
                "source_app": "com.test.app"
            },
            "navigation_meta": {
                "role": "registry_sensor",
                "session_state": "inactive"
            }
        }
        """.trimIndent()
        
        val packet = gson.fromJson(json, RegistryPacket::class.java)
        assertNotNull(packet)
        assertEquals("registry-mind-v1", packet.header.protocol)
        assertEquals("base64_blob", packet.payload.imageData)
        assertEquals("Test OCR", packet.payload.ocrContent)
        assertEquals("registry_sensor", packet.navigationMeta.role)
    }
}
