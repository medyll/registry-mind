package com.registry.mind.ui.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import com.registry.mind.ui.theme.RegistryMindTheme
import com.registry.mind.ui.components.PeripheralGlow

class CaptureOverlayManager(private val context: Context) {
    
    private val windowManager: WindowManager = 
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    private var overlayView: View? = null
    private var isShowing = false
    
    private val layoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
    
    fun showPeripheralGlow() {
        if (isShowing) return
        
        overlayView = ComposeView(context).apply {
            setContent {
                RegistryMindTheme {
                    PeripheralGlow(
                        onDismiss = { hideOverlay() }
                    )
                }
            }
        }
        
        try {
            windowManager.addView(overlayView, layoutParams)
            isShowing = true
            
            // Auto-dismiss after 500ms
            overlayView?.postDelayed({
                hideOverlay()
            }, 500)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun showLiquidButton(
        onCaptureTriggered: () -> Unit,
        onLongPress: () -> Unit
    ) {
        if (isShowing) return
        
        overlayView = ComposeView(context).apply {
            setContent {
                RegistryMindTheme {
                    LiquidButton(
                        onCaptureTriggered = onCaptureTriggered,
                        onLongPress = onLongPress
                    )
                }
            }
        }
        
        try {
            val buttonParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.END or Gravity.TOP
                y = 200 // Offset from top
            }
            
            windowManager.addView(overlayView, buttonParams)
            isShowing = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun hideOverlay() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
                overlayView = null
                isShowing = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun cleanup() {
        hideOverlay()
    }
}
