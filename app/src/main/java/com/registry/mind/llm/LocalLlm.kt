package com.registry.mind.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalLlm(private val context: Context) {

    private var inference: LlmInference? = null

    val isReady: Boolean get() = inference != null

    suspend fun init(modelFile: File) = withContext(Dispatchers.Default) {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelFile.absolutePath)
            .setMaxTokens(512)
            .setTopK(40)
            .setTemperature(0.7f)
            .setRandomSeed(42)
            .build()
        inference = LlmInference.createFromOptions(context, options)
    }

    suspend fun summarize(text: String): String = withContext(Dispatchers.Default) {
        val prompt = buildGemmaPrompt("Résume ce texte en 2-3 phrases courtes:\n$text")
        runInference(prompt)
    }

    suspend fun rewrite(text: String, instruction: String): String = withContext(Dispatchers.Default) {
        val prompt = buildGemmaPrompt("$instruction:\n$text")
        runInference(prompt)
    }

    private fun runInference(prompt: String): String {
        return inference?.generateResponse(prompt)?.trim() ?: ""
    }

    private fun buildGemmaPrompt(userText: String): String =
        "<start_of_turn>user\n$userText<end_of_turn>\n<start_of_turn>model\n"

    fun close() {
        inference?.close()
        inference = null
    }
}
