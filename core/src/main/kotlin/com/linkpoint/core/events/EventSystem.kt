package com.linkpoint.core.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Event system for the viewer, providing a centralized way to handle events across modules.
 * 
 * Based on event systems from:
 * - SecondLife viewer's LLEventPump and LLEventHost
 * - Firestorm viewer's event handling
 * - General observer pattern implementations
 */
sealed class ViewerEvent {
    // Connection events
    data class Connected(val sessionId: String) : ViewerEvent()
    data class Disconnected(val reason: String) : ViewerEvent()
    data class ConnectionFailed(val error: String) : ViewerEvent()
    
    // Avatar events
    data class AvatarMoved(val position: Vector3, val rotation: Quaternion) : ViewerEvent()
    data class AvatarTeleported(val region: String, val position: Vector3) : ViewerEvent()
    
    // Chat events
    data class ChatReceived(val message: String, val sender: String, val channel: Int) : ViewerEvent()
    data class InstantMessageReceived(val message: String, val sender: String) : ViewerEvent()
    
    // Object events
    data class ObjectAdded(val objectId: String, val properties: Map<String, Any>) : ViewerEvent()
    data class ObjectRemoved(val objectId: String) : ViewerEvent()
    data class ObjectUpdated(val objectId: String, val properties: Map<String, Any>) : ViewerEvent()
    
    // UI events
    data class WindowResized(val width: Int, val height: Int) : ViewerEvent()
    data class MenuActionTriggered(val action: String) : ViewerEvent()
}

/**
 * Simple 3D vector class for position data
 */
data class Vector3(val x: Float, val y: Float, val z: Float)

/**
 * Simple quaternion class for rotation data
 */
data class Quaternion(val x: Float, val y: Float, val z: Float, val w: Float)

/**
 * Centralized event system that allows modules to communicate through events
 */
object EventSystem {
    private val _events = MutableSharedFlow<ViewerEvent>(
        replay = 0,
        extraBufferCapacity = 1000
    )
    
    val events: SharedFlow<ViewerEvent> = _events.asSharedFlow()
    
    /**
     * Emit an event to all subscribers
     */
    suspend fun emit(event: ViewerEvent) {
        logger.debug { "Emitting event: ${event::class.simpleName}" }
        _events.emit(event)
    }
    
    /**
     * Emit an event without suspending (fire and forget)
     */
    fun tryEmit(event: ViewerEvent): Boolean {
        logger.debug { "Trying to emit event: ${event::class.simpleName}" }
        return _events.tryEmit(event)
    }
}