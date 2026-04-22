package com.registry.mind.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.registry.mind.session.SessionManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class VoiceProcessor(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    fun startListening(): Flow<VoiceResult> = callbackFlow {
        SessionManager.getInstance(context).startSession()

        recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)

        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                SessionManager.getInstance(context).resetTimeout()
                trySend(VoiceResult.Transcription(text, parseCommand(text)))
            }

            override fun onError(error: Int) {
                trySend(VoiceResult.Error(error))
            }

            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partial: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        recognizer?.startListening(intent)

        awaitClose { stop() }
    }

    fun stop() {
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
    }

    private fun parseCommand(text: String): VoiceCommand? {
        val lower = text.lowercase().trim()
        return when {
            lower.contains("pause") || lower.contains("stop") -> VoiceCommand.PAUSE
            lower.contains("sync") || lower.contains("send") -> VoiceCommand.SYNC
            lower.contains("new file") || lower.contains("nouveau") -> VoiceCommand.NEW_FILE
            lower.contains("flush") -> VoiceCommand.FLUSH
            else -> null
        }
    }
}

sealed class VoiceResult {
    data class Transcription(val text: String, val command: VoiceCommand?) : VoiceResult()
    data class Error(val code: Int) : VoiceResult()
}

enum class VoiceCommand {
    PAUSE, SYNC, NEW_FILE, FLUSH
}
