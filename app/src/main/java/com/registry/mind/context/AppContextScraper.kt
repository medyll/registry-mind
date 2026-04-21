package com.registry.mind.context

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AppContextScraper(private val context: Context) {
    
    companion object {
        private var accessibilityService: AccessibilityService? = null
        
        fun setService(service: AccessibilityService) {
            accessibilityService = service
        }
    }
    
    fun getCurrentPackage(): String {
        return try {
            val service = accessibilityService
            if (service != null) {
                val event = service.rootInActiveWindow
                event?.packageName?.toString() ?: context.packageName
            } else {
                context.packageName
            }
        } catch (e: Exception) {
            context.packageName
        }
    }
    
    fun getCurrentActivity(): String? {
        return try {
            val service = accessibilityService
            service?.rootInActiveWindow?.className?.toString()
        } catch (e: Exception) {
            null
        }
    }
    
    fun getWindowText(): String {
        return try {
            val service = accessibilityService
            val rootNode = service?.rootInActiveWindow ?: return ""
            
            val textBuilder = StringBuilder()
            collectTextFromNode(rootNode, textBuilder)
            textBuilder.toString()
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun collectTextFromNode(node: AccessibilityNodeInfo, builder: StringBuilder) {
        if (node.text != null) {
            builder.append(node.text).append("\n")
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                collectTextFromNode(child, builder)
                child.recycle()
            }
        }
    }
}
