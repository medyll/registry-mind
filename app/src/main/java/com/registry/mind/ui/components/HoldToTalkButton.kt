package com.registry.mind.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

enum class HoldToTalkState {
    IDLE,
    LISTENING,
    PROCESSING
}

@Composable
fun HoldToTalkButton(
    onRecordingStart: () -> Unit,
    onRecordingStop: (audioFile: java.io.File) -> Unit,
    onProcessingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var state by remember { mutableStateOf(HoldToTalkState.IDLE) }
    var currentAudioFile by remember { mutableStateOf<java.io.File?>(null) }
    
    val infiniteTransition = rememberInfiniteTransition()
    
    // Idle pulse animation
    val idleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // Listening waveform animation
    val waveformProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // Processing spinner rotation
    val processingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    val buttonColor = when (state) {
        HoldToTalkState.IDLE -> Color(0xFF2196F3) // Blue
        HoldToTalkState.LISTENING -> Color(0xFFF44336) // Red
        HoldToTalkState.PROCESSING -> Color(0xFFFFC107) // Yellow
    }
    
    Box(
        modifier = modifier
            .size(72.dp)
            .pointerInput(state) {
                if (state == HoldToTalkState.IDLE || state == HoldToTalkState.LISTENING) {
                    detectDragGestures(
                        onDragStart = {
                            if (state == HoldToTalkState.IDLE) {
                                state = HoldToTalkState.LISTENING
                                onRecordingStart()
                            }
                        },
                        onDragEnd = {
                            if (state == HoldToTalkState.LISTENING) {
                                state = HoldToTalkState.PROCESSING
                                // Simulate processing delay
                                kotlinx.coroutines.GlobalScope.launch {
                                    delay(1000)
                                    currentAudioFile?.let { file ->
                                        onRecordingStop(file)
                                    }
                                    state = HoldToTalkState.IDLE
                                    onProcessingComplete()
                                }
                            }
                        }
                    )
                }
            }
    ) {
        // Background circle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = buttonColor,
                    shape = CircleShape
                )
        )
        
        // Waveform animation (listening state)
        if (state == HoldToTalkState.LISTENING) {
            WaveformAnimation(progress = waveformProgress)
        }
        
        // Processing spinner
        if (state == HoldToTalkState.PROCESSING) {
            ProcessingSpinner(rotation = processingRotation)
        }
        
        // Microphone icon
        MicrophoneIcon(
            state = state,
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.Center)
        )
    }
}

@Composable
private fun WaveformAnimation(progress: Float) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        val barWidth = size.width / 7
        val maxBarHeight = size.height * 0.6f
        
        for (i in 0..4) {
            val barHeight = maxBarHeight * (0.3f + 0.7f * progress * (1 - Math.abs(i - 2) / 2f).toFloat())
            val x = size.width / 2 - (2 - i) * barWidth * 1.5f
            val y = (size.height - barHeight) / 2
            
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF64B5F6).copy(alpha = 0.6f),
                        Color(0xFF2196F3).copy(alpha = 0.9f)
                    )
                ),
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barWidth * 0.8f, barHeight)
            )
        }
    }
}

@Composable
private fun ProcessingSpinner(rotation: Float) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFFFC107).copy(alpha = 0.2f),
                    Color(0xFFFFC107)
                )
            ),
            startAngle = rotation - 90,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

@Composable
private fun MicrophoneIcon(state: HoldToTalkState, modifier: Modifier = Modifier) {
    // Simple microphone representation
    Box(
        modifier = modifier
            .background(
                color = if (state == HoldToTalkState.LISTENING) Color.White else Color.White.copy(alpha = 0.9f),
                shape = CircleShape
            )
    )
}
