package com.registry.mind.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.registry.mind.work.SyncManager

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            // MediaProjection FGS cannot be started at boot — requires live user consent.
            // Accessibility service (SnapKeyService) auto-restores via system settings.
            SyncManager.scheduleOneTimeSync(context)
        }
    }
}
