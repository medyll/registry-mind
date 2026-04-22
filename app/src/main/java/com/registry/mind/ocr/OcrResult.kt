package com.registry.mind.ocr

data class OcrResult(
    val fullText: String,
    val blocks: List<TextBlock>,
    val confidence: Float
)

data class TextBlock(
    val text: String,
    val lines: List<String>,
    val confidence: Float
)
