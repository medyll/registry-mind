package com.registry.mind

import com.registry.mind.voice.VoiceCommand
import com.registry.mind.voice.VoiceResult
import org.junit.Assert.*
import org.junit.Test

class VoiceProcessorTest {

    // Test command parsing logic in isolation (no Android dependency)
    private fun parseCommand(text: String): VoiceCommand? {
        val lower = text.lowercase().trim()
        return when {
            lower.contains("pause") || lower.contains("stop") -> VoiceCommand.PAUSE
            lower.contains("sync") || lower.contains("send") -> VoiceCommand.SYNC
            lower.contains("new file") || lower.contains("nouveau") -> VoiceCommand.NEW_FILE
            lower.contains("flush") -> VoiceCommand.FLUSH
            else -> null
        }
    }

    @Test
    fun `pause command parsed correctly`() {
        assertEquals(VoiceCommand.PAUSE, parseCommand("pause"))
        assertEquals(VoiceCommand.PAUSE, parseCommand("Stop everything"))
    }

    @Test
    fun `sync command parsed correctly`() {
        assertEquals(VoiceCommand.SYNC, parseCommand("sync now"))
        assertEquals(VoiceCommand.SYNC, parseCommand("send data"))
    }

    @Test
    fun `new file command parsed`() {
        assertEquals(VoiceCommand.NEW_FILE, parseCommand("new file"))
        assertEquals(VoiceCommand.NEW_FILE, parseCommand("nouveau document"))
    }

    @Test
    fun `unknown command returns null`() {
        assertNull(parseCommand("hello world"))
        assertNull(parseCommand(""))
    }

    @Test
    fun `VoiceResult Transcription holds text and command`() {
        val result = VoiceResult.Transcription("sync now", VoiceCommand.SYNC)
        assertEquals("sync now", result.text)
        assertEquals(VoiceCommand.SYNC, result.command)
    }
}
