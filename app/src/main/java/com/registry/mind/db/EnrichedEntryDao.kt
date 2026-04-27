package com.registry.mind.db

import androidx.room.*
import com.registry.mind.data.EnrichedEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface EnrichedEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: EnrichedEntry)

    @Query("SELECT * FROM enriched_entries ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<EnrichedEntry>>

    @Query("SELECT * FROM enriched_entries ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<EnrichedEntry>

    @Query("SELECT * FROM enriched_entries WHERE exportedAt IS NULL ORDER BY timestamp ASC")
    suspend fun getPendingExport(): List<EnrichedEntry>

    @Query("UPDATE enriched_entries SET exportedAt = :ts WHERE id = :id")
    suspend fun markExported(id: String, ts: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(entry: EnrichedEntry)

    @Query("DELETE FROM enriched_entries WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
