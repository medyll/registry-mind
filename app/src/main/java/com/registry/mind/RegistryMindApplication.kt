package com.registry.mind

import android.app.Application
import com.registry.mind.ingestor.CacheManager
import com.registry.mind.network.ClawConnector
import com.registry.mind.context.AppContextScraper

class RegistryMindApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize network layer
        ClawConnector.initialize(this)
        
        // Initialize cache manager
        CacheManager.initialize(this)
        
        // Set accessibility service reference
        // AppContextScraper.setService(accessibilityService) // Set when service connects
    }
    
    companion object {
        lateinit var instance: RegistryMindApplication
            private set
    }
}
