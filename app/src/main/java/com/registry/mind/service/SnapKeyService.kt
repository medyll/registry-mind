package com.registry.mind.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.registry.mind.ui.overlay.CaptureOverlayManager

class SnapKeyService : AccessibilityService() {
    
    companion object {
        const val ACTION_SNAP_KEY_SINGLE = "com.registry.mind.SNAP_KEY_SINGLE"
        const val ACTION_SNAP_KEY_LONG = "com.registry.mind.SNAP_KEY_LONG"
        
        private var instance: SnapKeyService? = null
        
        fun isRunning(): Boolean = instance != null
    }
    
    private val overlayManager by lazy { CaptureOverlayManager(this) }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                        AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                   AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                   AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        serviceInfo = info
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                if (isSnapKeyClick(event)) {
                    handleSnapKeyTrigger(event)
                }
            }
        }
    }
    
    private fun isSnapKeyClick(event: AccessibilityEvent): Boolean {
        // Detect Snap Key hardware button clicks
        // On Oppo Find X9, this would be tied to the physical button
        // For now, we detect specific click patterns
        return event.className?.contains("SnapKey") == true ||
               event.packageName == packageName
    }
    
    private fun handleSnapKeyTrigger(event: AccessibilityEvent) {
        when {
            isLongPress(event) -> {
                sendBroadcast(ACTION_SNAP_KEY_LONG)
            }
            else -> {
                sendBroadcast(ACTION_SNAP_KEY_SINGLE)
            }
        }
    }
    
    private fun isLongPress(event: AccessibilityEvent): Boolean {
        // Implement long press detection logic
        // This would check the duration of the button press
        return false // Placeholder
    }
    
    private fun sendBroadcast(action: String) {
        val intent = Intent(action).apply {
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }
    
    override fun onInterrupt() {
        instance = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        overlayManager.cleanup()
    }
}
