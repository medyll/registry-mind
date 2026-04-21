package com.registry.mind.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.registry.mind.R
import com.registry.mind.ui.MainActivity
import com.registry.mind.ui.overlay.CaptureOverlayManager
import com.registry.mind.ingestor.RegistryIngestor

class CaptureService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "capture_service_channel"
        
        private var instance: CaptureService? = null
        
        fun isRunning(): Boolean = instance != null
    }
    
    private lateinit var windowManager: WindowManager
    private lateinit var overlayManager: CaptureOverlayManager
    private lateinit var ingestor: RegistryIngestor
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayManager = CaptureOverlayManager(this)
        ingestor = RegistryIngestor(this)
        
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundService()
            ACTION_CAPTURE -> startCapture()
            ACTION_STOP -> stopService()
        }
        return START_STICKY
    }
    
    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun startCapture() {
        overlayManager.showPeripheralGlow()
        ingestor.captureAndSend()
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
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private fun stopService() {
        overlayManager.hideOverlay()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        overlayManager.cleanup()
    }
    
    companion object {
        const val ACTION_START = "com.registry.mind.START_CAPTURE"
        const val ACTION_CAPTURE = "com.registry.mind.CAPTURE"
        const val ACTION_STOP = "com.registry.mind.STOP_CAPTURE"
    }
}
