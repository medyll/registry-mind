package com.registry.mind.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Display
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ScreenCapturer(private val context: Context) {
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())
    
    fun setMediaProjection(projection: MediaProjection) {
        mediaProjection = projection
    }
    
    suspend fun capture(): Bitmap? = suspendCancellableCoroutine { continuation ->
        if (mediaProjection == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)

        @Suppress("DEPRECATION")
        val metrics = DisplayMetrics().also { display?.getMetrics(it) }
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi
        
        if (display == null) { continuation.resume(null); return@suspendCancellableCoroutine }
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        
        virtualDisplay = mediaProjection!!.createVirtualDisplay(
            "ScreenCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null,
            handler
        )
        
        handler.postDelayed({
            val image = imageReader?.acquireLatestImage()
            val bitmap = image?.toBitmap()
            
            image?.close()
            virtualDisplay?.release()
            
            continuation.resume(bitmap)
        }, 100)
    }
    
    fun release() {
        virtualDisplay?.release()
        imageReader?.close()
    }
    
    private fun Image.toBitmap(): Bitmap {
        val plane = planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * width
        
        val bitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        
        return bitmap
    }
}
