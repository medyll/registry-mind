package com.registry.mind.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class OcrProcessor {
    
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    suspend fun process(bitmap: Bitmap): OcrResult = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val result = OcrResult(
                    fullText = visionText.text,
                    blocks = visionText.textBlocks.map { block ->
                        OcrTextBlock(
                            text = block.text,
                            boundingBox = block.boundingBox,
                            lines = block.lines.map { line ->
                                OcrTextLine(
                                    text = line.text,
                                    boundingBox = line.boundingBox,
                                    confidence = line.confidence ?: 0f
                                )
                            }
                        )
                    },
                    success = true,
                    error = null
                )
                continuation.resume(result)
            }
            .addOnFailureListener { e ->
                continuation.resume(
                    OcrResult(
                        fullText = "",
                        blocks = emptyList(),
                        success = false,
                        error = e.message
                    )
                )
            }
    }
    
    fun close() {
        recognizer.close()
    }
}

data class OcrResult(
    val fullText: String,
    val blocks: List<OcrTextBlock>,
    val success: Boolean,
    val error: String?
)

data class OcrTextBlock(
    val text: String,
    val boundingBox: android.graphics.Rect?,
    val lines: List<OcrTextLine>
)

data class OcrTextLine(
    val text: String,
    val boundingBox: android.graphics.Rect?,
    val confidence: Float
)
