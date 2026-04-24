package com.registry.mind

import android.app.Application
import com.registry.mind.haptics.HapticFeedback
import com.registry.mind.ingestor.CacheManager
import com.registry.mind.network.ClawConnector
import com.registry.mind.settings.SettingsManager
import com.registry.mind.context.AppContextScraper

class RegistryMindApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize settings first (auth token + endpoint URL)
        SettingsManager.initialize(this)

        // Initialize network layer (reads from SettingsManager)
        ClawConnector.initialize(this)

        // Initialize cache manager
        CacheManager.initialize(this)

        // Initialize haptic feedback
        HapticFeedback.initialize(this)
        
        // Set accessibility service reference
        // AppContextScraper.setService(accessibilityService) // Set when service connects
    }
    
    companion object {
        lateinit var instance: RegistryMindApplication
            private set
    }
}
