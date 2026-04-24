package com.registry.mind.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class CaptureTag(val label: String, val emoji: String, val color: Color) {
    URGENT("Urgent",  "🔴", Color(0xFFF44336)),
    PERSONAL("Personal", "🔵", Color(0xFF2196F3)),
    WORK("Work", "🟢", Color(0xFF4CAF50))
}

/**
 * Radial menu with 3 quick tags.
 * Appears on long-press of LiquidButton, dismisses on selection or outside tap.
 */
@Composable
fun RadialTagMenu(
    onTagSelected: (CaptureTag) -> Unit,
    onDismiss: () -> Unit
) {
    val enterTransition = rememberInfiniteTransition(label = "radial")
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "menuScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss), // tap outside → dismiss
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .scale(scale)
                .background(
                    Color(0xFF1E1E1E).copy(alpha = 0.92f),
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .clickable { /* consume clicks inside the menu */ }
        ) {
            Text(
                text = "Quick tag",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            CaptureTag.entries.forEach { tag ->
                TagItem(tag = tag, onClick = {
                    onTagSelected(tag)
                    onDismiss()
                })
            }
        }
    }
}

@Composable
private fun TagItem(tag: CaptureTag, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(tag.color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(tag.color, CircleShape)
        )
        Text(
            text = tag.label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
