package com.registry.mind.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.registry.mind.data.NavigationMeta
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionManager private constructor(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "secure_session_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private var sessionJob: Job? = null
    private var isSessionActive = false
    private var sessionStartTime: Long = 0

    private val _sessionActiveFlow = MutableStateFlow(false)
    val sessionActiveFlow: StateFlow<Boolean> = _sessionActiveFlow.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    fun startSession() {
        if (isSessionActive) {
            resetTimeout()
            return
        }

        isSessionActive = true
        sessionStartTime = System.currentTimeMillis()
        _sessionActiveFlow.value = true

        sessionJob?.cancel()
        sessionJob = scope.launch {
            delay(SESSION_TIMEOUT_MS)
            if (isSessionActive) endSession()
        }

        prefs.edit().putBoolean(KEY_SESSION_ACTIVE, true).apply()
    }

    fun endSession() {
        isSessionActive = false
        sessionJob?.cancel()
        sessionJob = null
        _sessionActiveFlow.value = false

        prefs.edit().putBoolean(KEY_SESSION_ACTIVE, false).apply()
    }

    fun resetTimeout() {
        if (!isSessionActive) return

        sessionJob?.cancel()
        sessionJob = scope.launch {
            delay(SESSION_TIMEOUT_MS)
            if (isSessionActive) endSession()
        }
    }

    fun isSessionActive(): Boolean = isSessionActive

    fun getSessionState(): String = if (isSessionActive) "active_chat" else "inactive"

    fun createNavigationMeta(): NavigationMeta = NavigationMeta(
        role = "registry_sensor",
        sessionState = getSessionState()
    )

    fun wasSessionActiveOnRestart(): Boolean =
        prefs.getBoolean(KEY_SESSION_ACTIVE, false)

    companion object {
        private const val SESSION_TIMEOUT_MS = 30_000L
        private const val KEY_SESSION_ACTIVE = "session_active"

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
