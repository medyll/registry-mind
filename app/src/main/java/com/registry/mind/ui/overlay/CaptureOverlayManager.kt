package com.registry.mind.ui.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import com.registry.mind.session.SessionManager
import com.registry.mind.ui.theme.RegistryMindTheme
import com.registry.mind.ui.components.LiquidButton
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
            overlayView?.postDelayed({ hideOverlay() }, 500)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showLiquidButton(
        onCaptureTriggered: () -> Unit,
        onLongPress: () -> Unit
    ) {
        if (isShowing) return

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val buttonSizePx = (64 * displayMetrics.density).toInt()
        val edgePaddingPx = (16 * displayMetrics.density).toInt()

        val buttonParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - buttonSizePx - edgePaddingPx   // start: right edge
            y = 200
        }

        val sessionFlow = SessionManager.getInstance(context).sessionActiveFlow

        overlayView = ComposeView(context).apply {
            setContent {
                RegistryMindTheme {
                    val isSessionActive by sessionFlow.collectAsState()
                    LiquidButton(
                        onCaptureTriggered = onCaptureTriggered,
                        onLongPress = onLongPress,
                        isSessionActive = isSessionActive,
                        onDrag = { dx, dy ->
                            buttonParams.x = (buttonParams.x + dx.toInt())
                                .coerceIn(0, screenWidth - buttonSizePx)
                            buttonParams.y = (buttonParams.y + dy.toInt())
                                .coerceAtLeast(0)
                            try {
                                windowManager.updateViewLayout(overlayView, buttonParams)
                            } catch (_: Exception) {}
                        },
                        onDragEnd = {
                            // Snap to nearest horizontal edge
                            val center = buttonParams.x + buttonSizePx / 2
                            buttonParams.x = if (center < screenWidth / 2) {
                                edgePaddingPx
                            } else {
                                screenWidth - buttonSizePx - edgePaddingPx
                            }
                            try {
                                windowManager.updateViewLayout(overlayView, buttonParams)
                            } catch (_: Exception) {}
                        }
                    )
                }
            }
        }

        try {
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
