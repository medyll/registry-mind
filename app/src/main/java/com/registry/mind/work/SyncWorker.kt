package com.registry.mind.work

import android.content.Context
import androidx.work.*
import com.registry.mind.ingestor.CacheManager
import com.registry.mind.network.ClawConnector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    
    override fun doWork(): Result {
        return try {
            val pendingPackets = runBlocking {
                CacheManager.getPendingPackets()
            }
            
            if (pendingPackets.isEmpty()) {
                return Result.success()
            }
            
            var successCount = 0
            var failureCount = 0
            
            pendingPackets.forEach { packet ->
                val result = runBlocking {
                    ClawConnector.sendPacket(packet)
                }
                
                if (result.isSuccess) {
                    successCount++
                } else {
                    failureCount++
                }
            }
            
            if (failureCount == 0) {
                Result.success()
            } else if (successCount > 0) {
                Result.retry()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
    
    private fun runBlocking(block: suspend () -> Unit): Unit {
        kotlinx.coroutines.runBlocking { block() }
    }
}

object SyncManager {
    
    fun schedulePeriodicSync(context: Context) {
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
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
            "periodic_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
    
    fun scheduleOneTimeSync(context: Context) {
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueue(syncRequest)
    }
    
    fun cancelAllSync(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
    }
}
