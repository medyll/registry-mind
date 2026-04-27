package com.registry.mind.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "enriched_entries")
data class EnrichedEntry(
    @PrimaryKey val id: String,
    val rawText: String,
    val summary: String,
    val rewrite: String? = null,
    val sourceApp: String,
    val tag: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val exportedAt: Long? = null
)
