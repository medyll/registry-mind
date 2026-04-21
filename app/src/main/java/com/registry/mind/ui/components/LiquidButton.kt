package com.registry.mind.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.delay

@Composable
fun LiquidButton(
    onCaptureTriggered: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isAnchored by remember { mutableStateOf(true) }
    var position by remember { mutableStateOf(0f to 0f) }
    var isPressed by remember { mutableStateOf(false) }
    
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = modifier
            .size(64.dp)
            .scale(if (isPressed) 0.9f else pulseScale)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2196F3),
                        Color(0xFF1976D2)
                    )
                ),
                shape = CircleShape
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (!isAnchored) {
                            // Snap back to edge
                            isAnchored = true
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    position = (position.first + dragAmount.x) to (position.second + dragAmount.y)
                    isAnchored = false
                }
            }
            .pointerInput(Unit) {
                awaitFirstGesture {
                    detectLongPress(
                        onLongPress = {
                            onLongPress()
                        }
                    )
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    val event = awaitPointerEvent()
                    if (event.changes.any { it.pressed }) {
                        isPressed = true
                    } else if (isPressed) {
                        isPressed = false
                        onCaptureTriggered()
                    }
                }
            }
    )
}
