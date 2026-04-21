package com.registry.mind.network

import android.content.Context
import com.registry.mind.data.RegistryPacket
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ClawConnector {
    
    private var networkConfig: NetworkConfig? = null
    private var okHttpClient: OkHttpClient? = null
    private var api: ClawApiService? = null
    
    fun initialize(context: Context) {
        networkConfig = NetworkConfig(context)
        recreateClient()
    }
    
    private fun recreateClient() {
        val config = networkConfig ?: return
        
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("X-Registry-Auth", config.authToken)
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(config.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(config.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .writeTimeout(config.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        api = retrofit.create(ClawApiService::class.java)
    }
    
    suspend fun sendPacket(packet: RegistryPacket): Result<Unit> {
        val api = api ?: return Result.failure(Exception("ClawConnector not initialized"))
        
        return try {
            val config = networkConfig ?: return Result.failure(Exception("NetworkConfig not available"))
            val response = api.sendPacket(config.authToken, packet)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateConfig(baseUrl: String, authToken: String, timeoutSeconds: Int = 30): Result<Unit> {
        val config = networkConfig ?: return Result.failure(Exception("Not initialized"))
        
        return try {
            config.updateConfig(baseUrl, authToken, timeoutSeconds)
            recreateClient()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun isInitialized(): Boolean = networkConfig != null && api != null
}
