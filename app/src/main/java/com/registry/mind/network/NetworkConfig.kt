package com.registry.mind.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.registry.mind.data.RegistryPacket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NetworkConfig(private val context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val prefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "secure_network_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, "http://openclaw.local:8080") ?: "http://openclaw.local:8080"
        set(value) = prefs.edit().putString(KEY_BASE_URL, value).apply()
    
    var authToken: String
        get() = prefs.getString(KEY_AUTH_TOKEN, "YOUR_STATIC_API_KEY_HERE") ?: "YOUR_STATIC_API_KEY_HERE"
        set(value) = prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()
    
    var timeoutSeconds: Int
        get() = prefs.getInt(KEY_TIMEOUT, 30)
        set(value) = prefs.edit().putInt(KEY_TIMEOUT, value).apply()
    
    suspend fun updateConfig(baseUrl: String, authToken: String, timeout: Int = 30) = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            putString(KEY_BASE_URL, baseUrl)
            putString(KEY_AUTH_TOKEN, authToken)
            putInt(KEY_TIMEOUT, timeout)
        }.apply()
    }
    
    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_TIMEOUT = "timeout_seconds"
    }
}
