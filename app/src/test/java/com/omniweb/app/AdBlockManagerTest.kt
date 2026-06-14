package com.omniweb.app

import com.omniweb.app.util.AdBlockManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AdBlockManagerTest {

    @Test
    fun testGetCategory() {
        assertEquals("[Ad]", AdBlockManager.getCategory("doubleclick.net"))
        assertEquals("[Ad]", AdBlockManager.getCategory("ad.doubleclick.net"))
        assertEquals("[Analytics]", AdBlockManager.getCategory("google-analytics.com"))
        assertEquals("[Social]", AdBlockManager.getCategory("facebook.com"))
        assertNull(AdBlockManager.getCategory("google.com"))
        assertNull(AdBlockManager.getCategory("github.com"))
    }

    @Test
    fun testShouldBlock() {
        assertEquals(true, AdBlockManager.shouldBlock("doubleclick.net"))
        assertEquals(false, AdBlockManager.shouldBlock("google.com"))
    }

    @Test
    fun testGetAdBlockScript() {
        val script = AdBlockManager.getAdBlockScript()
        assertNotNull(script)
        assertEquals(true, script.contains("MutationObserver"))
        assertEquals(true, script.contains("display: none !important"))
    }
}
