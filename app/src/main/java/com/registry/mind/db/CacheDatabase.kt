package com.registry.mind.db

import android.content.Context
import androidx.room.*
import com.registry.mind.data.EnrichedEntry

@Entity(tableName = "cached_packets")
data class CachedPacket(
    @PrimaryKey val id: String,
    val json: String,
    val timestamp: Long = System.currentTimeMillis(),
    val retries: Int = 0
)

@Dao
interface CachedPacketDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(packet: CachedPacket)

    @Query("SELECT * FROM cached_packets ORDER BY timestamp ASC")
    suspend fun getAll(): List<CachedPacket>

    @Delete
    suspend fun delete(packet: CachedPacket)

    @Query("UPDATE cached_packets SET retries = retries + 1 WHERE id = :id")
    suspend fun incrementRetry(id: String)
}

@Database(entities = [CachedPacket::class, EnrichedEntry::class], version = 2, exportSchema = false)
abstract class CacheDatabase : RoomDatabase() {
    abstract fun packetDao(): CachedPacketDao
    abstract fun enrichedEntryDao(): EnrichedEntryDao

    companion object {
        @Volatile private var instance: CacheDatabase? = null

        fun get(context: Context): CacheDatabase = instance ?: synchronized(this) {
            Room.databaseBuilder(context, CacheDatabase::class.java, "registry_cache")
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}
