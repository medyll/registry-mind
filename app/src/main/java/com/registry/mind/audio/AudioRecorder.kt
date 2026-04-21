package com.registry.mind.audio

import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AudioRecorder(private val outputDir: File) {
    
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var isRecording = false
    
    fun startRecording(): Result<File> {
        return try {
            currentOutputFile = File(outputDir, "voice_${System.currentTimeMillis()}.m4a")
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(outputDir.context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(currentOutputFile!!.absolutePath)
                prepare()
                start()
            }
            
            isRecording = true
            Result.success(currentOutputFile!!)
        } catch (e: IOException) {
            isRecording = false
            Result.failure(e)
        } catch (e: IllegalStateException) {
            isRecording = false
            Result.failure(e)
        }
    }
    
    suspend fun stopRecording(): Result<File> = suspendCancellableCoroutine { continuation ->
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            
            mediaRecorder = null
            isRecording = false
            
            if (currentOutputFile != null && currentOutputFile!!.exists()) {
                continuation.resume(Result.success(currentOutputFile!!))
            } else {
                continuation.resume(Result.failure(Exception("Output file not created")))
            }
        } catch (e: RuntimeException) {
            isRecording = false
            continuation.resume(Result.failure(e))
        }
    }
    
    fun isRecording(): Boolean = isRecording
    
    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            currentOutputFile?.delete()
        } catch (e: Exception) {
            // Ignore errors during cancel
        } finally {
            mediaRecorder = null
            isRecording = false
            currentOutputFile = null
        }
    }
}
