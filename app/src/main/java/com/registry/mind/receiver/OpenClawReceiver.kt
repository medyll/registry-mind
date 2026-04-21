package com.registry.mind.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.registry.mind.service.CaptureService
import com.registry.mind.work.SyncManager

class OpenClawReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SIGNAL -> {
                handleActionRequired(context, intent)
            }
            ACTION_SYNC -> {
                triggerSync(context)
            }
            ACTION_FLUSH -> {
                flushCache(context)
            }
        }
    }
    
    private fun handleActionRequired(context: Context, intent: Intent) {
        val messageType = intent.getStringExtra("message_type") ?: "notification"
        val content = intent.getStringExtra("content") ?: ""
        
        Toast.makeText(
            context,
            "OpenClaw: $content",
            Toast.LENGTH_LONG
        ).show()
    }
    
    private fun triggerSync(context: Context) {
        SyncManager.scheduleOneTimeSync(context)
    }
    
    private fun flushCache(context: Context) {
        SyncManager.scheduleOneTimeSync(context)
    }
    
    companion object {
        const val ACTION_SIGNAL = "com.registry.mind.ACTION_SIGNAL"
        const val ACTION_SYNC = "com.registry.mind.ACTION_SYNC"
        const val ACTION_FLUSH = "com.registry.mind.ACTION_FLUSH"
    }
}
