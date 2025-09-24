package com.linkpoint.core

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the ViewerCore class - the main viewer lifecycle manager
 */
class ViewerCoreTest {
    
    @Test
    fun `should initialize successfully`() = runTest {
        val viewerCore = ViewerCore()
        
        assertFalse(viewerCore.isInitialized())
        assertFalse(viewerCore.isRunning())
        
        val result = viewerCore.initialize()
        
        assertTrue(result, "ViewerCore should initialize successfully")
        assertTrue(viewerCore.isInitialized(), "ViewerCore should be marked as initialized")
        assertFalse(viewerCore.isRunning(), "ViewerCore should not be running after init")
    }
    
    @Test
    fun `should start successfully after initialization`() = runTest {
        val viewerCore = ViewerCore()
        
        viewerCore.initialize()
        val result = viewerCore.start()
        
        assertTrue(result, "ViewerCore should start successfully")
        assertTrue(viewerCore.isRunning(), "ViewerCore should be marked as running")
    }
    
    @Test
    fun `should fail to start without initialization`() = runTest {
        val viewerCore = ViewerCore()
        
        val result = viewerCore.start()
        
        assertFalse(result, "ViewerCore should fail to start without initialization")
        assertFalse(viewerCore.isRunning(), "ViewerCore should not be running")
    }
    
    @Test
    fun `should shutdown gracefully`() = runTest {
        val viewerCore = ViewerCore()
        
        viewerCore.initialize()
        viewerCore.start()
        
        assertTrue(viewerCore.isRunning(), "ViewerCore should be running before shutdown")
        
        viewerCore.shutdown()
        
        assertFalse(viewerCore.isRunning(), "ViewerCore should not be running after shutdown")
    }
    
    @Test
    fun `should not initialize twice`() = runTest {
        val viewerCore = ViewerCore()
        
        val firstResult = viewerCore.initialize()
        val secondResult = viewerCore.initialize()
        
        assertTrue(firstResult, "First initialization should succeed")
        assertTrue(secondResult, "Second initialization should return true but not re-initialize")
        assertTrue(viewerCore.isInitialized(), "ViewerCore should remain initialized")
    }
}