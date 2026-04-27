package com.registry.mind.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.registry.mind.R
import com.registry.mind.haptics.HapticFeedback
import com.registry.mind.ingestor.RegistryIngestor
import com.registry.mind.llm.LocalLlm
import com.registry.mind.llm.ModelDownloadManager
import com.registry.mind.ui.MainActivity
import com.registry.mind.ui.overlay.CaptureOverlayManager
import com.registry.mind.ui.overlay.GlowState
import com.registry.mind.work.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CaptureService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "capture_service_channel"

        const val ACTION_START   = "com.registry.mind.START_CAPTURE"
        const val ACTION_CAPTURE = "com.registry.mind.CAPTURE"
        const val ACTION_STOP    = "com.registry.mind.STOP_CAPTURE"

        private var instance: CaptureService? = null
        fun isRunning(): Boolean = instance != null
    }

    private lateinit var windowManager: WindowManager
    private lateinit var overlayManager: CaptureOverlayManager
    private lateinit var ingestor: RegistryIngestor
    private var localLlm: LocalLlm? = null

    // Main dispatcher: overlay calls (showVetoBar etc.) must run on Main
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        instance = this

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayManager = CaptureOverlayManager(this)
        ingestor = RegistryIngestor(this)

        createNotificationChannel()
        SyncManager.schedulePeriodicSync(this)
        initLocalLlmIfReady()
    }

    private fun initLocalLlmIfReady() {
        val modelFile = ModelDownloadManager(this).modelFile
        if (!modelFile.exists()) return
        serviceScope.launch(Dispatchers.Default) {
            val llm = LocalLlm(this@CaptureService)
            runCatching { llm.init(modelFile) }
                .onSuccess {
                    localLlm = llm
                    ingestor.localLlm = llm
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START   -> startForegroundService()
            ACTION_CAPTURE -> startCapture()
            ACTION_STOP    -> stopService()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        startForeground(NOTIFICATION_ID, createNotification())
    }

    /**
     * Full capture flow:
     *  1. Haptic tick + peripheral glow
     *  2. captureOnly() on IO (screenshot + OCR)
     *  3. Show VetoTempoBar
     *  4a. onCommit → sendPacket → haptic success / error
     *  4b. onVeto   → discard silently
     */
    private fun startCapture() {
        HapticFeedback.captureInitiated()
        overlayManager.showPeripheralGlow(GlowState.CAPTURING)

        serviceScope.launch {
            val result = withContext(Dispatchers.IO) {
                ingestor.captureOnly()
            }

            result.onSuccess { packet ->
                overlayManager.showVetoBar(
                    onCommit = {
                        serviceScope.launch(Dispatchers.IO) {
                            ingestor.processPacket(packet)
                            HapticFeedback.syncSuccessful()
                            overlayManager.showPeripheralGlow(GlowState.SYNCED)
                        }
                    },
                    onVeto = {
                        // Discarded — no processing
                    }
                )
            }.onFailure {
                HapticFeedback.errorOccurred()
                overlayManager.showPeripheralGlow(GlowState.ERROR)
            }
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.capture_notification_title))
            .setContentText(getString(R.string.capture_notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(createPendingIntent())
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Capture Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background service for screen capture"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun stopService() {
        overlayManager.cleanup()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
        overlayManager.cleanup()
        ingestor.cleanup()
        localLlm?.close()
    }
}
