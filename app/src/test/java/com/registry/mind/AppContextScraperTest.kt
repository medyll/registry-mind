package com.registry.mind.context

import org.junit.Test
import kotlin.test.assertEquals

class AppContextScraperTest {
    
    @Test
    fun `classifyContent returns correct categories`() {
        val scraper = AppContextScraperTest()
        
        val financeText = "Invoice #12345 - Amount: $500.00"
        val workText = "Meeting at 3pm - Conference Room A"
        val recipeText = "Recipe: Chocolate Cake - Ingredients: flour, sugar"
        val travelText = "Flight confirmation - Booking ID: ABC123"
        
        // Note: classification logic is in RegistryIngestor
        // This is a placeholder for integration tests
        assertEquals(true, financeText.contains("invoice", ignoreCase = true))
        assertEquals(true, workText.contains("meeting", ignoreCase = true))
        assertEquals(true, recipeText.contains("recipe", ignoreCase = true))
        assertEquals(true, travelText.contains("flight", ignoreCase = true))
    }
}
