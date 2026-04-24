package com.registry.mind.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.registry.mind.ui.overlay.GlowState
import kotlinx.coroutines.delay

@Composable
fun PeripheralGlow(
    state: GlowState = GlowState.CAPTURING,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(true) }
    val infiniteTransition = rememberInfiniteTransition(label = "glow")

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    LaunchedEffect(state) {
        delay(state.durationMs)
        visible = false
        onDismiss()
    }

    if (visible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = glowAlpha }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                state.color.copy(alpha = 0.4f),
                                state.color.copy(alpha = 0.7f),
                                state.color.copy(alpha = 0.4f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .blur(8.dp)
            )
        }
    }
}
