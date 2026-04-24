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
import com.registry.mind.ui.components.CaptureTag
import com.registry.mind.ui.components.LiquidButton
import com.registry.mind.ui.components.PeripheralGlow
import com.registry.mind.ui.components.RadialTagMenu
import com.registry.mind.ui.components.VetoTempoBar

class CaptureOverlayManager(private val context: Context) {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // --- Glow / LiquidButton overlay ---
    private var overlayView: View? = null
    private var isShowing = false

    // --- VetoTempo overlay (independent slot) ---
    private var vetoView: View? = null
    private var isVetoShowing = false

    // --- Radial tag menu overlay (independent slot) ---
    private var radialView: View? = null
    private var isRadialShowing = false

    private val fullscreenParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
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

    // -------------------------------------------------------------------------
    // Peripheral glow
    // -------------------------------------------------------------------------

    fun showPeripheralGlow(state: GlowState = GlowState.CAPTURING) {
        if (isShowing) return

        overlayView = ComposeView(context).apply {
            setContent {
                RegistryMindTheme {
                    PeripheralGlow(state = state, onDismiss = { hideOverlay() })
                }
            }
        }

        try {
            windowManager.addView(overlayView, fullscreenParams)
            isShowing = true
            // PeripheralGlow self-dismisses via LaunchedEffect after state.durationMs
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // -------------------------------------------------------------------------
    // Liquid button (persistent floating button)
    // -------------------------------------------------------------------------

    fun showLiquidButton(
        onCaptureTriggered: () -> Unit
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
            x = screenWidth - buttonSizePx - edgePaddingPx
            y = 200
        }

        val session = SessionManager.getInstance(context)
        val sessionFlow = session.sessionActiveFlow

        overlayView = ComposeView(context).apply {
            setContent {
                RegistryMindTheme {
                    val isSessionActive by sessionFlow.collectAsState()
                    LiquidButton(
                        onCaptureTriggered = onCaptureTriggered,
                        onLongPress = { showRadialTagMenu(session) },
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

    // -------------------------------------------------------------------------
    // Veto Tempo Bar (bottom overlay, independent of glow/button)
    // -------------------------------------------------------------------------

    fun showVetoBar(onCommit: () -> Unit, onVeto: () -> Unit) {
        if (isVetoShowing) return

        val vetoParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        vetoView = ComposeView(context).apply {
            setContent {
                RegistryMindTheme {
                    VetoTempoBar(
                        onCommit = {
                            hideVetoBar()
                            onCommit()
                        },
                        onVeto = {
                            hideVetoBar()
                            onVeto()
                        }
                    )
                }
            }
        }

        try {
            windowManager.addView(vetoView, vetoParams)
            isVetoShowing = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideVetoBar() {
        vetoView?.let { view ->
            try {
                windowManager.removeView(view)
                vetoView = null
                isVetoShowing = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Radial tag menu
    // -------------------------------------------------------------------------

    private fun showRadialTagMenu(session: com.registry.mind.session.SessionManager) {
        if (isRadialShowing) return

        radialView = ComposeView(context).apply {
            setContent {
                RegistryMindTheme {
                    RadialTagMenu(
                        onTagSelected = { tag: CaptureTag ->
                            session.currentTag = tag.label
                        },
                        onDismiss = { hideRadialMenu() }
                    )
                }
            }
        }

        try {
            windowManager.addView(radialView, fullscreenParams)
            isRadialShowing = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideRadialMenu() {
        radialView?.let { view ->
            try {
                windowManager.removeView(view)
                radialView = null
                isRadialShowing = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

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
        hideVetoBar()
        hideRadialMenu()
    }
}
