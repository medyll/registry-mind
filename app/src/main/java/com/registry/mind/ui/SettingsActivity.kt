package com.registry.mind.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.registry.mind.network.ClawConnector
import com.registry.mind.settings.SettingsManager
import com.registry.mind.ui.theme.RegistryMindTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RegistryMindTheme {
                SettingsScreen(onClose = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()

    // --- State ---
    var endpointUrl by remember { mutableStateOf(SettingsManager.getEndpointUrl()) }
    var authToken   by remember { mutableStateOf(SettingsManager.getAuthToken()) }
    var timeoutSec  by remember { mutableStateOf(SettingsManager.getTimeoutSeconds().toString()) }
    var hapticsOn   by remember { mutableStateOf(SettingsManager.getHapticsEnabled()) }

    var tokenVisible  by remember { mutableStateOf(false) }
    var isSaved       by remember { mutableStateOf(false) }
    var testStatus    by remember { mutableStateOf<TestResult?>(null) }
    var isTesting     by remember { mutableStateOf(false) }

    val isConfigured = authToken.isNotBlank() && endpointUrl.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onClose) {
                        Text("← Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // --- Status badge ---
            StatusBadge(isConfigured = isConfigured, isSaved = isSaved)

            // --- Connectivity section ---
            SettingsSection(title = "Connectivity") {

                SettingsField(
                    label = "Endpoint URL",
                    value = endpointUrl,
                    onValueChange = { endpointUrl = it; isSaved = false },
                    placeholder = SettingsManager.DEFAULT_ENDPOINT,
                    hint = "Tailscale MagicDNS or IP:port"
                )

                SettingsField(
                    label = "Auth Token",
                    value = authToken,
                    onValueChange = { authToken = it; isSaved = false },
                    placeholder = "X-Registry-Auth value",
                    hint = "Stored encrypted on-device",
                    visualTransformation = if (tokenVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { tokenVisible = !tokenVisible }) {
                            Text(if (tokenVisible) "Hide" else "Show", fontSize = 12.sp)
                        }
                    },
                    keyboardType = KeyboardType.Password
                )

                SettingsField(
                    label = "Timeout (seconds)",
                    value = timeoutSec,
                    onValueChange = { timeoutSec = it.filter { c -> c.isDigit() }; isSaved = false },
                    placeholder = "30",
                    hint = "Connection + read timeout",
                    keyboardType = KeyboardType.Number
                )
            }

            // --- Test connection ---
            TestConnectionBlock(
                endpointUrl = endpointUrl,
                isTesting = isTesting,
                testStatus = testStatus,
                onTest = {
                    isTesting = true
                    testStatus = null
                    scope.launch {
                        testStatus = pingEndpoint(endpointUrl)
                        isTesting = false
                    }
                }
            )

            // --- Behavior section ---
            SettingsSection(title = "Behavior") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Haptic Feedback",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Vibrate on capture, sync, error",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = hapticsOn,
                        onCheckedChange = { hapticsOn = it; isSaved = false }
                    )
                }
            }

            // --- Save button ---
            Button(
                onClick = {
                    val timeout = timeoutSec.toIntOrNull() ?: 30
                    SettingsManager.setEndpointUrl(endpointUrl.trim())
                    SettingsManager.setAuthToken(authToken.trim())
                    SettingsManager.setTimeoutSeconds(timeout)
                    SettingsManager.setHapticsEnabled(hapticsOn)
                    ClawConnector.refreshConfig()
                    isSaved = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isConfigured,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isSaved) "✓ Saved" else "Save",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Warn if auth token is empty
            AnimatedVisibility(!isConfigured) {
                Text(
                    text = "⚠ Auth token and endpoint URL are required before captures can be sent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Sub-composables
// ---------------------------------------------------------------------------

@Composable
private fun StatusBadge(isConfigured: Boolean, isSaved: Boolean) {
    val (bgColor, label) = if (isConfigured)
        Color(0xFF4CAF50) to "● Ready"
    else
        Color(0xFFF44336) to "● Not configured"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = bgColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        if (isSaved) {
            Text(
                text = "Changes saved",
                color = Color(0xFF4CAF50),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    hint: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )
        if (hint.isNotBlank()) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TestConnectionBlock(
    endpointUrl: String,
    isTesting: Boolean,
    testStatus: TestResult?,
    onTest: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onTest,
            modifier = Modifier.fillMaxWidth(),
            enabled = endpointUrl.isNotBlank() && !isTesting,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isTesting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Testing…")
            } else {
                Text("Test Connection")
            }
        }

        testStatus?.let { result ->
            val (color, message) = when (result) {
                is TestResult.Success -> Color(0xFF4CAF50) to "✓ Reachable (${result.ms}ms)"
                is TestResult.Failure -> Color(0xFFF44336) to "✗ ${result.reason}"
            }
            Text(
                text = message,
                color = color,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Network probe
// ---------------------------------------------------------------------------

sealed class TestResult {
    data class Success(val ms: Long) : TestResult()
    data class Failure(val reason: String) : TestResult()
}

private suspend fun pingEndpoint(rawUrl: String): TestResult = withContext(Dispatchers.IO) {
    try {
        val url = rawUrl.trimEnd('/') + "/health"
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        val t0 = System.currentTimeMillis()
        val response = client.newCall(
            Request.Builder().url(url).get().build()
        ).execute()
        val ms = System.currentTimeMillis() - t0

        // Accept any HTTP response (even 404) — endpoint is reachable
        response.close()
        TestResult.Success(ms)
    } catch (e: Exception) {
        TestResult.Failure(e.message ?: "Unknown error")
    }
}
