package com.registry.mind.ocr

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OcrProcessorTest {
    
    @Test
    fun `OcrResult handles success case`() {
        val result = OcrResult(
            fullText = "Hello World",
            blocks = listOf(
                OcrTextBlock(
                    text = "Hello",
                    boundingBox = null,
                    lines = listOf(
                        OcrTextLine(text = "Hello", boundingBox = null, confidence = 0.95f)
                    )
                )
            ),
            success = true,
            error = null
        )
        
        assertEquals("Hello World", result.fullText)
        assertTrue(result.success)
        assertEquals(1, result.blocks.size)
    }
    
    @Test
    fun `OcrResult handles error case`() {
        val result = OcrResult(
            fullText = "",
            blocks = emptyList(),
            success = false,
            error = "ML Kit processing failed"
        )
        
        assertTrue(result.fullText.isEmpty())
        assertTrue(!result.success)
        assertEquals("ML Kit processing failed", result.error)
    }
}
