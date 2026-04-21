package com.registry.mind.ingestor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import com.registry.mind.data.*
import com.registry.mind.network.ClawConnector
import com.registry.mind.ocr.OcrProcessor
import com.registry.mind.screen.ScreenCapturer
import com.registry.mind.context.AppContextScraper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant

class RegistryIngestor(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val screenCapturer = ScreenCapturer(context)
    private val ocrProcessor = OcrProcessor()
    private val contextScraper = AppContextScraper(context)
    
    private var isReady = false
    
    fun initialize(mediaProjectionIntent: Intent): Boolean {
        return try {
            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val mediaProjection = projectionManager.getMediaProjection(Activity.RESULT_OK, mediaProjectionIntent)
            
            screenCapturer.setMediaProjection(mediaProjection)
            isReady = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun captureAndSend() {
        if (!isReady) {
            CacheManager.storeForRetry(
                RegistryPacket(
                    header = Header(
                        protocol = "registry-mind-v1",
                        device = "Oppo_Find_X9_Native",
                        timestamp = Instant.now().toString(),
                        authToken = "YOUR_STATIC_API_KEY_HERE"
                    ),
                    payload = Payload(
                        imageData = "",
                        ocrContent = "",
                        sourceApp = context.packageName,
                        aiGuess = null
                    ),
                    navigationMeta = NavigationMeta(
                        role = "registry_sensor",
                        sessionState = "error_not_initialized"
                    )
                )
            )
            return
        }
        
        scope.launch {
            try {
                val bitmap = screenCapturer.capture()
                
                if (bitmap == null) {
                    CacheManager.storeForRetry(createEmptyPacket("capture_failed"))
                    return@launch
                }
                
                val ocrResult = ocrProcessor.process(bitmap)
                val sourceApp = contextScraper.getCurrentPackage()
                
                if (!ocrResult.success) {
                    CacheManager.storeForRetry(createEmptyPacket("ocr_failed"))
                    return@launch
                }
                
                val packet = RegistryPacket(
                    header = Header(
                        protocol = "registry-mind-v1",
                        device = "Oppo_Find_X9_Native",
                        timestamp = Instant.now().toString(),
                        authToken = "YOUR_STATIC_API_KEY_HERE"
                    ),
                    payload = Payload(
                        imageData = bitmapToBase64(bitmap),
                        ocrContent = ocrResult.fullText,
                        sourceApp = sourceApp,
                        aiGuess = classifyContent(ocrResult.fullText)
                    ),
                    navigationMeta = NavigationMeta(
                        role = "registry_sensor",
                        sessionState = "inactive"
                    )
                )
                
                val result = ClawConnector.sendPacket(packet)
                
                result.onSuccess {
                    // Success - green haptic feedback
                }.onFailure { error ->
                    CacheManager.storeForRetry(packet)
                }
                
                bitmap.recycle()
                
            } catch (e: Exception) {
                e.printStackTrace()
                CacheManager.storeForRetry(createEmptyPacket("ingestor_error: ${e.message}"))
            }
        }
    }
    
    private fun createEmptyPacket(error: String): RegistryPacket {
        return RegistryPacket(
            header = Header(
                protocol = "registry-mind-v1",
                device = "Oppo_Find_X9_Native",
                timestamp = Instant.now().toString(),
                authToken = "YOUR_STATIC_API_KEY_HERE"
            ),
            payload = Payload(
                imageData = "",
                ocrContent = "",
                sourceApp = context.packageName,
                aiGuess = null
            ),
            navigationMeta = NavigationMeta(
                role = "registry_sensor",
                sessionState = "error:$error"
            )
        )
    }
    
    private fun classifyContent(ocrText: String): String? {
        return when {
            ocrText.contains("invoice", ignoreCase = true) || 
            ocrText.contains("facture", ignoreCase = true) ||
            ocrText.contains("receipt", ignoreCase = true) -> "finance_document"
            ocrText.contains("meeting", ignoreCase = true) ||
            ocrText.contains("calendar", ignoreCase = true) -> "work_meeting"
            ocrText.contains("recipe", ignoreCase = true) ||
            ocrText.contains("ingredients", ignoreCase = true) -> "personal_recipe"
            ocrText.contains("flight", ignoreCase = true) ||
            ocrText.contains("hotel", ignoreCase = true) ||
            ocrText.contains("booking", ignoreCase = true) -> "travel"
            else -> null
        }
    }
    
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArray = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.WEBP, 85, byteArray)
        return android.util.Base64.encodeToString(byteArray.toByteArray(), android.util.Base64.NO_WRAP)
    }
    
    fun cleanup() {
        screenCapturer.release()
        ocrProcessor.close()
    }
}
