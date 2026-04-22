package com.registry.mind

import com.registry.mind.ocr.OcrResult
import com.registry.mind.ocr.TextBlock
import org.junit.Assert.*
import org.junit.Test

class OcrResultTest {

    @Test
    fun `OcrResult stores fullText and blocks`() {
        val result = OcrResult(
            fullText = "Hello\nWorld",
            blocks = listOf(
                TextBlock(text = "Hello", lines = listOf("Hello"), confidence = 0.99f),
                TextBlock(text = "World", lines = listOf("World"), confidence = 0.95f)
            ),
            confidence = 0.97f
        )
        assertEquals("Hello\nWorld", result.fullText)
        assertEquals(2, result.blocks.size)
        assertEquals(0.99f, result.blocks[0].confidence)
    }

    @Test
    fun `empty OCR result has empty fullText`() {
        val result = OcrResult(fullText = "", blocks = emptyList(), confidence = 0f)
        assertTrue(result.fullText.isEmpty())
        assertTrue(result.blocks.isEmpty())
    }
}
