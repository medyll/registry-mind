package com.registry.mind.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun VetoTempoBar(
    onCommit: () -> Unit,
    onVeto: () -> Unit,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableStateOf(0f) }
    var visible by remember { mutableStateOf(true) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    LaunchedEffect(Unit) {
        val steps = 30 // 3 seconds / 100ms
        for (i in 1..steps) {
            delay(100)
            progress = i.toFloat() / steps
            if (progress >= 1f) {
                visible = false
                onCommit()
                break
            }
        }
    }
    
    if (visible) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF4CAF50).copy(alpha = glowAlpha),
                            Color(0xFF8BC34A).copy(alpha = glowAlpha)
                        )
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable {
                    visible = false
                    onVeto()
                }
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tap to cancel • Auto-sending in ${3 - (progress * 30).toInt()}s",
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .background(Color.White, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}
