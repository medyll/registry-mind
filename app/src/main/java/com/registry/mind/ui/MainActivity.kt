package com.registry.mind.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.registry.mind.llm.ModelDownloadManager
import com.registry.mind.service.CaptureService
import com.registry.mind.settings.SettingsManager
import com.registry.mind.ui.theme.RegistryMindTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RegistryMindTheme {
                AppNavHost()
            }
        }
    }
}

@Composable
fun AppNavHost() {
    var showEntries by remember { mutableStateOf(false) }
    if (showEntries) {
        EntriesScreen(onBack = { showEntries = false })
    } else {
        MainScreen(onShowEntries = { showEntries = true })
    }
}

@Composable
fun MainScreen(onShowEntries: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isConfigured by remember { mutableStateOf(SettingsManager.hasAuthToken()) }
    var isServiceRunning by remember { mutableStateOf(CaptureService.isRunning()) }

    val downloadManager = remember { ModelDownloadManager(context) }
    var modelReady by remember { mutableStateOf(downloadManager.isDownloaded) }
    var showDownloadDialog by remember { mutableStateOf(!downloadManager.isDownloaded) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isConfigured = SettingsManager.hasAuthToken()
                isServiceRunning = CaptureService.isRunning()
                modelReady = downloadManager.isDownloaded
                if (!modelReady) showDownloadDialog = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showDownloadDialog && !modelReady) {
        ModelDownloadDialog(
            progress = downloadProgress,
            error = downloadError,
            onStart = {
                downloadError = null
                scope.launch {
                    val modelUrl = SettingsManager.getModelUrl()
                    downloadManager.download(modelUrl) { p -> downloadProgress = p }
                        .onSuccess {
                            modelReady = true
                            showDownloadDialog = false
                        }
                        .onFailure { e ->
                            downloadError = e.message ?: "Download failed"
                            downloadProgress = 0f
                        }
                }
            },
            onDismiss = { showDownloadDialog = false }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            Spacer(modifier = Modifier.weight(1f))

            // --- Title ---
            Text(
                text = "Registry Mind",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sovereign cognitive sensor",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- Config status badge ---
            ConfigStatusBadge(isConfigured = isConfigured)

            Spacer(modifier = Modifier.height(24.dp))

            // --- Service status ---
            ServiceStatusCard(isRunning = isServiceRunning)

            Spacer(modifier = Modifier.height(12.dp))

            // --- Model status ---
            ModelStatusBadge(
                modelReady = modelReady,
                onDownload = { showDownloadDialog = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- Capture hint ---
            if (isConfigured && modelReady) {
                Text(
                    text = "Press Snap Key to capture",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- Captures button ---
            Button(
                onClick = onShowEntries,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("View Captures", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Settings button ---
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "⚙  Settings",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ConfigStatusBadge(isConfigured: Boolean) {
    val (bgColor, text) = if (isConfigured)
        Color(0xFF4CAF50) to "● Connected & ready"
    else
        Color(0xFFF44336) to "● Not configured — open Settings"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            color = bgColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ServiceStatusCard(isRunning: Boolean) {
    val (color, label, sub) = if (isRunning)
        Triple(Color(0xFF4CAF50), "Service: Active", "Background capture enabled")
    else
        Triple(Color(0xFFFF9800), "Service: Stopped", "Will start on next Snap Key press")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, RoundedCornerShape(50))
            )
            Column {
                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(sub, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ModelStatusBadge(modelReady: Boolean, onDownload: () -> Unit) {
    val (bgColor, text) = if (modelReady)
        Color(0xFF4CAF50) to "● AI model ready"
    else
        Color(0xFFFF9800) to "● AI model not downloaded"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = bgColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        if (!modelReady) {
            TextButton(onClick = onDownload, contentPadding = PaddingValues(0.dp)) {
                Text("Download", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ModelDownloadDialog(
    progress: Float,
    error: String?,
    onStart: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI Model Required") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Gemma 2B-IT (~1.3 GB) needed for on-device summarization.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (progress > 0f && error == null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = onStart, enabled = progress == 0f || error != null) {
                Text(if (error != null) "Retry" else "Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later") }
        }
    )
}
