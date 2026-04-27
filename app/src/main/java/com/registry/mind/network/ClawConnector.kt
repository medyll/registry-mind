package com.registry.mind.network

import android.content.Context
import com.registry.mind.BuildConfig
import com.registry.mind.data.RegistryPacket
import com.registry.mind.settings.SettingsManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ClawConnector {

    private var api: ClawApiService? = null

    fun initialize(context: Context) {
        // SettingsManager must be initialized before this call (done in Application.onCreate)
        recreateClient()
    }

    /** Call after SettingsManager values change (new URL or token). */
    fun refreshConfig() {
        recreateClient()
    }

    private fun recreateClient() {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("X-Registry-Auth", SettingsManager.getAuthToken())
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(SettingsManager.getTimeoutSeconds().toLong(), TimeUnit.SECONDS)
            .readTimeout(SettingsManager.getTimeoutSeconds().toLong(), TimeUnit.SECONDS)
            .writeTimeout(SettingsManager.getTimeoutSeconds().toLong(), TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(SettingsManager.getEndpointUrl())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(ClawApiService::class.java)
    }

    suspend fun sendPacket(packet: RegistryPacket): Result<Unit> {
        val api = api ?: return Result.failure(Exception("ClawConnector not initialized"))

        return try {
            val response = api.sendPacket(SettingsManager.getAuthToken(), packet)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Update endpoint + token at runtime, then refresh Retrofit client. */
    fun updateConfig(endpointUrl: String, authToken: String, timeoutSeconds: Int = 30) {
        SettingsManager.setEndpointUrl(endpointUrl)
        SettingsManager.setAuthToken(authToken)
        SettingsManager.setTimeoutSeconds(timeoutSeconds)
        recreateClient()
    }

    fun isInitialized(): Boolean = api != null
}
