package com.linkpoint.core.events

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the EventSystem - centralized event handling
 */
class EventSystemTest {
    
    @Test
    fun `should emit and receive events`() = runTest {
        val testEvent = ViewerEvent.Connected("test-session-123")
        
        // Start collecting events
        val eventJob = launch {
            val receivedEvent = EventSystem.events.first()
            assertEquals(testEvent, receivedEvent, "Should receive the same event that was emitted")
        }
        
        // Emit the event
        EventSystem.emit(testEvent)
        
        // Wait for the event to be processed
        eventJob.join()
    }
    
    @Test
    fun `should handle multiple event types`() = runTest {
        val events = mutableListOf<ViewerEvent>()
        
        // Collect events
        val eventJob = launch {
            EventSystem.events.collect { event ->
                events.add(event)
                if (events.size >= 3) return@collect
            }
        }
        
        // Emit different types of events
        EventSystem.emit(ViewerEvent.Connected("session-1"))
        EventSystem.emit(ViewerEvent.ChatReceived("Hello", "TestUser", 0))
        EventSystem.emit(ViewerEvent.Disconnected("User quit"))
        
        eventJob.join()
        
        assertEquals(3, events.size, "Should receive all 3 events")
        assertTrue(events[0] is ViewerEvent.Connected, "First event should be Connected")
        assertTrue(events[1] is ViewerEvent.ChatReceived, "Second event should be ChatReceived")
        assertTrue(events[2] is ViewerEvent.Disconnected, "Third event should be Disconnected")
    }
    
    @Test
    fun `should support tryEmit for non-suspending emission`() {
        val testEvent = ViewerEvent.AvatarMoved(
            Vector3(1.0f, 2.0f, 3.0f),
            Quaternion(0.0f, 0.0f, 0.0f, 1.0f)
        )
        
        val result = EventSystem.tryEmit(testEvent)
        
        assertTrue(result, "tryEmit should succeed")
    }
    
    @Test
    fun `should handle Vector3 and Quaternion data correctly`() = runTest {
        val position = Vector3(10.5f, 20.3f, 15.7f)
        val rotation = Quaternion(0.1f, 0.2f, 0.3f, 0.9f)
        val testEvent = ViewerEvent.AvatarMoved(position, rotation)
        
        val eventJob = launch {
            val receivedEvent = EventSystem.events.first() as ViewerEvent.AvatarMoved
            
            assertEquals(position.x, receivedEvent.position.x, "X position should match")
            assertEquals(position.y, receivedEvent.position.y, "Y position should match") 
            assertEquals(position.z, receivedEvent.position.z, "Z position should match")
            
            assertEquals(rotation.x, receivedEvent.rotation.x, "X rotation should match")
            assertEquals(rotation.y, receivedEvent.rotation.y, "Y rotation should match")
            assertEquals(rotation.z, receivedEvent.rotation.z, "Z rotation should match")
            assertEquals(rotation.w, receivedEvent.rotation.w, "W rotation should match")
        }
        
        EventSystem.emit(testEvent)
        eventJob.join()
    }
}