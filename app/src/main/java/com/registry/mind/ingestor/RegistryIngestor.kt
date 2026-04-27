package com.registry.mind.ingestor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import com.registry.mind.data.*
import com.registry.mind.db.CacheDatabase
import com.registry.mind.export.ExportConnector
import com.registry.mind.llm.LocalLlm
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
import java.util.UUID

class RegistryIngestor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val screenCapturer = ScreenCapturer(context)
    private val ocrProcessor = OcrProcessor()
    private val contextScraper = AppContextScraper(context)
    private val db = CacheDatabase.get(context)

    var localLlm: LocalLlm? = null
    val exportConnectors = mutableListOf<ExportConnector>()

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
     * Main entry point. Capture → OCR → LLM enrich → store locally.
     * Export connectors run async after storage.
     */
    fun captureAndProcess() {
        if (!isReady) return
        val session = SessionManager.getInstance(context)
        session.resetTimeout()

        scope.launch {
            captureOnly()
                .onSuccess { packet -> processAndStore(packet) }
                .onFailure { /* silent — OCR failure non-critical */ }
        }
    }

    private suspend fun processAndStore(packet: RegistryPacket) {
        val rawText = packet.payload.ocrContent
        if (rawText.isBlank()) return

        val summary = localLlm?.takeIf { it.isReady }?.summarize(rawText) ?: rawText.take(200)
        val session = SessionManager.getInstance(context)

        val entry = EnrichedEntry(
            id = UUID.randomUUID().toString(),
            rawText = rawText,
            summary = summary,
            sourceApp = packet.payload.sourceApp,
            tag = packet.navigationMeta.tag,
            timestamp = System.currentTimeMillis()
        )

        db.enrichedEntryDao().insert(entry)

        // fire-and-forget export
        exportConnectors.forEach { connector ->
            scope.launch {
                connector.export(entry)
            }
        }
    }

    /** Raw capture + OCR. Returns RegistryPacket with ocrContent populated. */
    suspend fun captureOnly(): Result<RegistryPacket> {
        val session = SessionManager.getInstance(context)
        if (!isReady) return Result.failure(IllegalStateException("not_initialized"))
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
                    aiGuess = null
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

    fun cleanup() {
        screenCapturer.release()
        ocrProcessor.close()
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArray = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.WEBP, 85, byteArray)
        return android.util.Base64.encodeToString(byteArray.toByteArray(), android.util.Base64.NO_WRAP)
    }
}
