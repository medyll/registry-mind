package com.registry.mind.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Singleton owner of all persistent config:
 *  - auth token (replaces hardcoded "YOUR_STATIC_API_KEY_HERE")
 *  - endpoint URL (Tailscale MagicDNS or manual IP fallback)
 *  - timeout
 *
 * Initialize once in RegistryMindApplication.onCreate().
 */
object SettingsManager {

    private const val PREFS_NAME          = "secure_settings"
    private const val KEY_AUTH_TOKEN      = "auth_token"
    private const val KEY_ENDPOINT_URL    = "endpoint_url"
    private const val KEY_TIMEOUT_SECONDS = "timeout_seconds"
    private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
    private const val KEY_MODEL_URL       = "model_url"

    const val DEFAULT_ENDPOINT  = "http://mydde-pc.tail70869a.ts.net"
    // Real source: https://www.kaggle.com/models/google/gemma/tfLite/gemma-2b-it-gpu-int4
    // Download manually and push: adb push model.bin /data/data/com.registry.mind/files/gemma-2b-it-q4.bin
    const val DEFAULT_MODEL_URL = ""
    private const val DEFAULT_TIMEOUT = 30

    private lateinit var prefs: SharedPreferences

    fun initialize(context: Context) {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // --- Auth token ---

    fun getAuthToken(): String =
        prefs.getString(KEY_AUTH_TOKEN, "") ?: ""

    fun setAuthToken(token: String) =
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()

    fun hasAuthToken(): Boolean = getAuthToken().isNotBlank()

    // --- Endpoint URL ---

    fun getEndpointUrl(): String =
        prefs.getString(KEY_ENDPOINT_URL, DEFAULT_ENDPOINT) ?: DEFAULT_ENDPOINT

    fun setEndpointUrl(url: String) =
        prefs.edit().putString(KEY_ENDPOINT_URL, url).apply()

    // --- Timeout ---

    fun getTimeoutSeconds(): Int =
        prefs.getInt(KEY_TIMEOUT_SECONDS, DEFAULT_TIMEOUT)

    fun setTimeoutSeconds(seconds: Int) =
        prefs.edit().putInt(KEY_TIMEOUT_SECONDS, seconds).apply()

    // --- Haptics ---

    fun getHapticsEnabled(): Boolean =
        prefs.getBoolean(KEY_HAPTICS_ENABLED, true)

    fun setHapticsEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, enabled).apply()

    // --- Model URL ---

    fun getModelUrl(): String =
        prefs.getString(KEY_MODEL_URL, DEFAULT_MODEL_URL) ?: DEFAULT_MODEL_URL

    fun setModelUrl(url: String) =
        prefs.edit().putString(KEY_MODEL_URL, url).apply()
}
