package com.registry.mind.ingestor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import com.registry.mind.data.*
import com.registry.mind.network.ClawConnector
import com.registry.mind.ocr.OcrProcessor
import com.registry.mind.screen.ScreenCapturer
import com.registry.mind.context.AppContextScraper
import com.registry.mind.session.SessionManager
import com.registry.mind.settings.SettingsManager
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

    /**
     * Capture screen + OCR. Returns a ready-to-send RegistryPacket.
     * Must be called from a background dispatcher (IO).
     */
    suspend fun captureOnly(): Result<RegistryPacket> {
        val session = SessionManager.getInstance(context)

        if (!isReady) {
            return Result.failure(IllegalStateException("not_initialized"))
        }

        session.resetTimeout()

        return try {
            val bitmap = screenCapturer.capture()
                ?: return Result.failure(RuntimeException("capture_failed"))

            val ocrResult = ocrProcessor.process(bitmap)
            val sourceApp = contextScraper.getCurrentPackage()

            if (!ocrResult.success) {
                bitmap.recycle()
                return Result.failure(RuntimeException("ocr_failed"))
            }

            val packet = RegistryPacket(
                header = Header(
                    protocol = "registry-mind-v1",
                    device = "Oppo_Find_X9_Native",
                    timestamp = Instant.now().toString(),
                    authToken = SettingsManager.getAuthToken()
                ),
                payload = Payload(
                    imageData = bitmapToBase64(bitmap),
                    ocrContent = ocrResult.fullText,
                    sourceApp = sourceApp,
                    aiGuess = classifyContent(ocrResult.fullText)
                ),
                navigationMeta = session.createNavigationMeta()
            )

            bitmap.recycle()
            Result.success(packet)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Send a previously captured packet to ClawConnector.
     * Caches on failure. Must be called from a background dispatcher (IO).
     */
    suspend fun sendPacket(packet: RegistryPacket): Result<Unit> {
        return try {
            val result = ClawConnector.sendPacket(packet)
            result.fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { error ->
                    CacheManager.storeForRetry(packet)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            CacheManager.storeForRetry(packet)
            Result.failure(e)
        }
    }

    /** Convenience: capture + send in one shot (used by legacy call sites). */
    fun captureAndSend() {
        val session = SessionManager.getInstance(context)

        if (!isReady) {
            scope.launch { CacheManager.storeForRetry(
                RegistryPacket(
                    header = Header(
                        protocol = "registry-mind-v1",
                        device = "Oppo_Find_X9_Native",
                        timestamp = Instant.now().toString(),
                        authToken = SettingsManager.getAuthToken()
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
            ) }
            return
        }

        session.resetTimeout()

        scope.launch {
            captureOnly()
                .onSuccess { packet -> sendPacket(packet) }
                .onFailure { e -> CacheManager.storeForRetry(createEmptyPacket("ingestor_error: ${e.message}")) }
        }
    }

    private fun createEmptyPacket(error: String): RegistryPacket = RegistryPacket(
        header = Header(
            protocol = "registry-mind-v1",
            device = "Oppo_Find_X9_Native",
            timestamp = Instant.now().toString(),
            authToken = SettingsManager.getAuthToken()
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

    private fun classifyContent(ocrText: String): String? = when {
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
