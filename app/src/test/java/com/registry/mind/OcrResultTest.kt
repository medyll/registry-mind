package com.registry.mind

import com.registry.mind.ocr.OcrResult
import com.registry.mind.ocr.OcrTextBlock
import com.registry.mind.ocr.OcrTextLine
import org.junit.Assert.*
import org.junit.Test

class OcrResultTest {

    @Test
    fun `OcrResult stores fullText and blocks`() {
        val result = OcrResult(
            fullText = "Hello\nWorld",
            blocks = listOf(
                OcrTextBlock(
                    text = "Hello",
                    boundingBox = null,
                    lines = listOf(OcrTextLine(text = "Hello", boundingBox = null, confidence = 0.99f))
                ),
                OcrTextBlock(
                    text = "World",
                    boundingBox = null,
                    lines = listOf(OcrTextLine(text = "World", boundingBox = null, confidence = 0.95f))
                )
            ),
            success = true,
            error = null
        )
        assertEquals("Hello\nWorld", result.fullText)
        assertEquals(2, result.blocks.size)
        assertEquals(0.99f, result.blocks[0].lines[0].confidence)
        assertTrue(result.success)
        assertNull(result.error)
    }

    @Test
    fun `empty OCR result has empty fullText`() {
        val result = OcrResult(fullText = "", blocks = emptyList(), success = true, error = null)
        assertTrue(result.fullText.isEmpty())
        assertTrue(result.blocks.isEmpty())
    }

    @Test
    fun `OcrResult failure carries error message`() {
        val result = OcrResult(fullText = "", blocks = emptyList(), success = false, error = "ocr_failed")
        assertFalse(result.success)
        assertEquals("ocr_failed", result.error)
    }
}
