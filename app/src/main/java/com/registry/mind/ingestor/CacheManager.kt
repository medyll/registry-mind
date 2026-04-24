package com.registry.mind.ingestor

import android.content.Context
import com.google.gson.Gson
import com.registry.mind.data.RegistryPacket
import com.registry.mind.db.CacheDatabase
import com.registry.mind.db.CachedPacket
import com.registry.mind.work.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object CacheManager {

    private lateinit var appContext: Context
    private val gson = Gson()

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun db(): CacheDatabase = CacheDatabase.get(appContext)

    /** Store a packet for retry and schedule an immediate WorkManager sync. */
    suspend fun storeForRetry(packet: RegistryPacket) = withContext(Dispatchers.IO) {
        val json = gson.toJson(packet)
        db().packetDao().insert(
            CachedPacket(
                id = UUID.randomUUID().toString(),
                json = json,
                timestamp = System.currentTimeMillis(),
                retries = 0
            )
        )
        // Trigger one-time sync as soon as network is available
        SyncManager.scheduleOneTimeSync(appContext)
    }

    suspend fun getPendingPackets(): List<Pair<String, RegistryPacket>> =
        withContext(Dispatchers.IO) {
            db().packetDao().getAll().mapNotNull { entity ->
                try {
                    val packet = gson.fromJson(entity.json, RegistryPacket::class.java)
                    entity.id to packet
                } catch (_: Exception) {
                    null // malformed entry — skip silently
                }
            }
        }

    suspend fun markAsSent(id: String) = withContext(Dispatchers.IO) {
        db().packetDao().delete(CachedPacket(id = id, json = ""))
    }

    suspend fun incrementRetry(id: String) = withContext(Dispatchers.IO) {
        db().packetDao().incrementRetry(id)
    }

    suspend fun deleteOldPackets(daysOld: Int) = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - daysOld.toLong() * 24 * 60 * 60 * 1000L
        db().packetDao().getAll()
            .filter { it.timestamp < cutoff }
            .forEach { db().packetDao().delete(it) }
    }
}
