package com.registry.mind.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.registry.mind.settings.SettingsManager

object HapticFeedback {
    
    private var vibrator: Vibrator? = null

    /** Reads live from SettingsManager — toggle takes effect immediately. */
    private val isEnabled: Boolean get() = SettingsManager.getHapticsEnabled()

    fun initialize(context: Context) {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    fun captureInitiated() {
        if (!isEnabled) return
        vibrator?.vibrate(
            VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }
    
    fun syncSuccessful() {
        if (!isEnabled) return
        vibrator?.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0, 50, 30, 50), // Double pulse
                -1
            )
        )
    }
    
    fun errorOccurred() {
        if (!isEnabled) return
        vibrator?.vibrate(
            VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }
    
    fun voiceCaptureStart() {
        if (!isEnabled) return
        vibrator?.vibrate(
            VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }
    
    fun voiceCaptureEnd() {
        if (!isEnabled) return
        vibrator?.vibrate(
            VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }
}
