package com.registry.mind.work

import android.content.Context
import androidx.work.*
import com.registry.mind.haptics.HapticFeedback
import com.registry.mind.ingestor.CacheManager
import com.registry.mind.network.ClawConnector
import java.util.concurrent.TimeUnit

/**
 * CoroutineWorker that flushes cached packets to ClawConnector.
 * - Runs only when network is CONNECTED (WorkManager constraint)
 * - Exponential backoff on partial failure
 * - Deletes each packet individually after successful send
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val pending = CacheManager.getPendingPackets()

            if (pending.isEmpty()) return Result.success()

            var allSuccess = true

            pending.forEach { (id, packet) ->
                val result = ClawConnector.sendPacket(packet)
                if (result.isSuccess) {
                    CacheManager.markAsSent(id)
                } else {
                    CacheManager.incrementRetry(id)
                    allSuccess = false
                }
            }

            if (allSuccess) {
                HapticFeedback.syncSuccessful()
                Result.success()
            } else {
                Result.retry() // exponential backoff will kick in
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

object SyncManager {

    private const val UNIQUE_ONE_TIME = "sync_on_demand"
    private const val UNIQUE_PERIODIC = "sync_periodic"

    /**
     * Enqueue a one-time sync triggered after storeForRetry().
     * Deduplicated — multiple calls while network is down don't pile up.
     */
    fun scheduleOneTimeSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30_000L, // initial 30s
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_ONE_TIME,
            ExistingWorkPolicy.KEEP, // don't replace if already queued
            request
        )
    }

    /** Periodic safety net — catches any packets missed by on-demand sync. */
    fun schedulePeriodicSync(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancelAllSync(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
    }
}
