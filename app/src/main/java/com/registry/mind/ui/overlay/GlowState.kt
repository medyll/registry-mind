package com.registry.mind.ui.overlay

import androidx.compose.ui.graphics.Color

enum class GlowState(val color: Color, val durationMs: Long) {
    CAPTURING(Color(0xFF2196F3), 500L),   // Blue  — capture in progress
    SYNCED(Color(0xFF4CAF50), 800L),      // Green — sync successful
    ERROR(Color(0xFFF44336), 800L)        // Red   — error / cache stored
}
