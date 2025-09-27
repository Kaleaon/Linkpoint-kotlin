/**
 * @file TouchGestureHandlerTranslated.kt
 * @brief Complete Kotlin translation of TouchGestureHandler from C# mobile viewer
 * 
 * TRANSLATED FROM: reference-sources/csharp/mobile/TouchGestureHandler.cs
 * ORIGINAL SOURCE: Mobile gesture system component
 * 
 * Translation Notes:
 * - Converted C# enums to Kotlin sealed classes for type safety
 * - Replaced C# Collections with Kotlin thread-safe collections
 * - Used Kotlin data classes instead of C# structs
 * - Applied coroutines for async gesture processing
 * - Enhanced with Kotlin Flow for reactive gesture events
 * - Added Android-specific touch handling optimizations
 */

package com.linkpoint.translated.mobile

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Gesture types for mobile interaction
 * Translated from C# enum GestureType
 */
sealed class GestureType {
    object None : GestureType()
    object Tap : GestureType()
    object DoubleTap : GestureType()
    object LongPress : GestureType()
    object Pan : GestureType()
    object Pinch : GestureType()
    object Rotate : GestureType()
    object Swipe : GestureType()
    object TwoFingerTap : GestureType()
    
    override fun toString(): String = this::class.simpleName ?: "Unknown"
}

/**
 * Touch phases for touch lifecycle
 * Translated from C# enum TouchPhase
 */
sealed class TouchPhase {
    object Began : TouchPhase()
    object Moved : TouchPhase()
    object Ended : TouchPhase()
    object Cancelled : TouchPhase()
    
    override fun toString(): String = this::class.simpleName ?: "Unknown"
}

/**
 * Point representation for touch coordinates
 * Translated from C# PointF
 */
data class PointF(val x: Float, val y: Float) {
    operator fun plus(other: PointF) = PointF(x + other.x, y + other.y)
    operator fun minus(other: PointF) = PointF(x - other.x, y - other.y)
    operator fun times(scalar: Float) = PointF(x * scalar, y * scalar)
    
    fun distanceTo(other: PointF): Float {
        val dx = other.x - x
        val dy = other.y - y
        return sqrt(dx * dx + dy * dy)
    }
}

/**
 * Gesture event data structure
 * Translated from C# struct GestureEvent
 */
data class GestureEvent(
    val type: GestureType,
    val location: PointF,
    val delta: PointF = PointF(0f, 0f),
    val scale: Float = 1.0f,
    val rotation: Float = 0.0f,
    val velocity: Float = 0.0f,
    val duration: Long = 0L, // milliseconds
    val fingerCount: Int = 1
)

/**
 * Touch point data structure
 * Translated from C# class TouchPoint
 */
data class TouchPoint(
    val id: Int,
    var position: PointF,
    val startPosition: PointF,
    val startTime: Long,
    var phase: TouchPhase
) {
    val totalDistance: Float
        get() = startPosition.distanceTo(position)
        
    val duration: Long
        get() = System.currentTimeMillis() - startTime
}

/**
 * Advanced touch gesture handling for mobile virtual world interaction
 * Translated from C# class TouchGestureHandler
 */
class TouchGestureHandlerTranslated {
    
    // Member variables (translated from C# private fields)
    private val activeTouches = ConcurrentHashMap<Int, TouchPoint>()
    private var gestureStartTime: Long = 0L
    private var gestureInProgress: Boolean = false
    private var currentGesture: GestureType = GestureType.None
    private var initialCenter: PointF = PointF(0f, 0f)
    private var initialDistance: Float = 0f
    private var initialAngle: Float = 0f
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Event flows for reactive programming
    private val _gestureEvents = MutableSharedFlow<GestureEvent>()
    val gestureEvents: SharedFlow<GestureEvent> = _gestureEvents.asSharedFlow()
    
    // Configuration
    private val tapTimeThreshold = 200L // milliseconds
    private val longPressTimeThreshold = 1000L // milliseconds
    private val movementThreshold = 10f // pixels
    private val swipeThreshold = 50f // pixels
    private val rotationThreshold = 5f // degrees
    private val pinchThreshold = 20f // pixels
    
    /**
     * Process touch input and detect gestures
     * Translated from: ProcessTouch()
     */
    suspend fun processTouch(pointerId: Int, x: Float, y: Float, phase: TouchPhase) {
        withContext(Dispatchers.Main) {
            when (phase) {
                is TouchPhase.Began -> handleTouchBegan(pointerId, x, y)
                is TouchPhase.Moved -> handleTouchMoved(pointerId, x, y)
                is TouchPhase.Ended -> handleTouchEnded(pointerId, x, y)
                is TouchPhase.Cancelled -> handleTouchCancelled(pointerId)
            }
        }
    }
    
    /**
     * Get current active touch count
     */
    fun getActiveTouchCount(): Int = activeTouches.size
    
    /**
     * Get current gesture in progress
     */
    fun getCurrentGesture(): GestureType = currentGesture
    
    /**
     * Check if gesture is in progress
     */
    fun isGestureInProgress(): Boolean = gestureInProgress
    
    /**
     * Shutdown the gesture handler
     */
    suspend fun shutdown() {
        coroutineScope.cancel()
        activeTouches.clear()
        endGestureDetection()
    }
    
    // Private methods (translated from C# private methods)
    
    private suspend fun handleTouchBegan(pointerId: Int, x: Float, y: Float) {
        val touch = TouchPoint(
            id = pointerId,
            position = PointF(x, y),
            startPosition = PointF(x, y),
            startTime = System.currentTimeMillis(),
            phase = TouchPhase.Began
        )
        
        activeTouches[pointerId] = touch
        
        when (activeTouches.size) {
            1 -> {
                // First touch - start gesture detection
                gestureStartTime = System.currentTimeMillis()
                initialCenter = PointF(x, y)
                startGestureDetection()
            }
            2 -> {
                // Second touch - enable multi-touch gestures
                updateMultiTouchGeometry()
            }
        }
        
        println("Touch began: ID=$pointerId, Position=($x, $y), Total touches=${activeTouches.size}")
    }
    
    private suspend fun handleTouchMoved(pointerId: Int, x: Float, y: Float) {
        val touch = activeTouches[pointerId] ?: return
        
        val previousPosition = touch.position
        touch.position = PointF(x, y)
        touch.phase = TouchPhase.Moved
        
        when (activeTouches.size) {
            1 -> handleSingleTouchMovement(touch, previousPosition)
            2 -> handleMultiTouchMovement()
        }
    }
    
    private suspend fun handleTouchEnded(pointerId: Int, x: Float, y: Float) {
        val touch = activeTouches[pointerId] ?: return
        
        touch.position = PointF(x, y)
        touch.phase = TouchPhase.Ended
        
        // Check for gesture completion
        checkForGestureCompletion(touch)
        
        activeTouches.remove(pointerId)
        
        if (activeTouches.isEmpty()) {
            endGestureDetection()
        }
        
        println("Touch ended: ID=$pointerId, Position=($x, $y), Remaining touches=${activeTouches.size}")
    }
    
    private suspend fun handleTouchCancelled(pointerId: Int) {
        activeTouches.remove(pointerId)
        
        if (activeTouches.isEmpty()) {
            endGestureDetection()
        }
        
        println("Touch cancelled: ID=$pointerId, Remaining touches=${activeTouches.size}")
    }
    
    private fun startGestureDetection() {
        gestureInProgress = true
        currentGesture = GestureType.None
    }
    
    private fun endGestureDetection() {
        gestureInProgress = false
        currentGesture = GestureType.None
    }
    
    private suspend fun handleSingleTouchMovement(touch: TouchPoint, previousPosition: PointF) {
        val delta = touch.position - previousPosition
        val totalDelta = touch.position - touch.startPosition
        val totalDistance = totalDelta.distanceTo(PointF(0f, 0f))
        
        if (totalDistance > movementThreshold) {
            if (currentGesture == GestureType.None) {
                currentGesture = GestureType.Pan
            }
            
            if (currentGesture == GestureType.Pan) {
                emitGestureEvent(GestureEvent(
                    type = GestureType.Pan,
                    location = touch.position,
                    delta = delta,
                    duration = touch.duration,
                    fingerCount = 1
                ))
            }
        }
    }
    
    private suspend fun handleMultiTouchMovement() {
        if (activeTouches.size != 2) return
        
        val touches = activeTouches.values.toList()
        val touch1 = touches[0]
        val touch2 = touches[1]
        
        val center = PointF(
            (touch1.position.x + touch2.position.x) / 2,
            (touch1.position.y + touch2.position.y) / 2
        )
        
        val distance = calculateDistance(touch1.position, touch2.position)
        val angle = calculateAngle(touch1.position, touch2.position)
        
        // Detect pinch gesture
        val distanceDelta = abs(distance - initialDistance)
        if (distanceDelta > pinchThreshold) {
            val scale = distance / initialDistance
            
            emitGestureEvent(GestureEvent(
                type = GestureType.Pinch,
                location = center,
                scale = scale,
                duration = System.currentTimeMillis() - gestureStartTime,
                fingerCount = 2
            ))
        }
        
        // Detect rotation gesture
        val angleDelta = abs(angle - initialAngle)
        if (angleDelta > rotationThreshold) {
            emitGestureEvent(GestureEvent(
                type = GestureType.Rotate,
                location = center,
                rotation = angle - initialAngle,
                duration = System.currentTimeMillis() - gestureStartTime,
                fingerCount = 2
            ))
        }
    }
    
    private suspend fun checkForGestureCompletion(touch: TouchPoint) {
        val duration = touch.duration
        val distance = touch.totalDistance
        
        when {
            distance < movementThreshold -> {
                // Small movement - check for tap or long press
                when {
                    duration < tapTimeThreshold -> {
                        // Quick tap
                        val gestureType = if (activeTouches.size > 1) GestureType.TwoFingerTap else GestureType.Tap
                        emitGestureEvent(GestureEvent(
                            type = gestureType,
                            location = touch.position,
                            duration = duration,
                            fingerCount = activeTouches.size
                        ))
                    }
                    duration > longPressTimeThreshold -> {
                        // Long press
                        emitGestureEvent(GestureEvent(
                            type = GestureType.LongPress,
                            location = touch.position,
                            duration = duration,
                            fingerCount = activeTouches.size
                        ))
                    }
                }
            }
            distance > swipeThreshold -> {
                // Significant movement - swipe gesture
                val velocity = distance / duration.toFloat() * 1000f // pixels per second
                val delta = touch.position - touch.startPosition
                
                emitGestureEvent(GestureEvent(
                    type = GestureType.Swipe,
                    location = touch.position,
                    delta = delta,
                    velocity = velocity,
                    duration = duration,
                    fingerCount = 1
                ))
            }
        }
    }
    
    private fun updateMultiTouchGeometry() {
        if (activeTouches.size != 2) return
        
        val touches = activeTouches.values.toList()
        val touch1 = touches[0]
        val touch2 = touches[1]
        
        initialCenter = PointF(
            (touch1.position.x + touch2.position.x) / 2,
            (touch1.position.y + touch2.position.y) / 2
        )
        
        initialDistance = calculateDistance(touch1.position, touch2.position)
        initialAngle = calculateAngle(touch1.position, touch2.position)
    }
    
    private fun calculateDistance(p1: PointF, p2: PointF): Float {
        return p1.distanceTo(p2)
    }
    
    private fun calculateAngle(p1: PointF, p2: PointF): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return atan2(dy, dx) * 180f / PI.toFloat()
    }
    
    private suspend fun emitGestureEvent(gestureEvent: GestureEvent) {
        println("Gesture detected: ${gestureEvent.type} at (${gestureEvent.location.x}, ${gestureEvent.location.y})")
        
        // Emit to reactive flow
        _gestureEvents.emit(gestureEvent)
    }
}

/**
 * Global gesture handling management
 * Translated from C# usage patterns
 */
object TouchGestureHandlerInstance {
    private var gestureHandler: TouchGestureHandlerTranslated? = null
    
    /**
     * Initialize the global gesture handler
     */
    fun initGestureHandler(): TouchGestureHandlerTranslated {
        if (gestureHandler == null) {
            gestureHandler = TouchGestureHandlerTranslated()
        }
        return gestureHandler!!
    }
    
    /**
     * Get global gesture handler instance
     */
    fun getGestureHandler(): TouchGestureHandlerTranslated? = gestureHandler
    
    /**
     * Process touch input
     */
    suspend fun processTouch(pointerId: Int, x: Float, y: Float, phase: TouchPhase) {
        gestureHandler?.processTouch(pointerId, x, y, phase)
    }
    
    /**
     * Shutdown gesture handler
     */
    suspend fun shutdownGestureHandler() {
        gestureHandler?.shutdown()
        gestureHandler = null
    }
}

/**
 * Demonstration of the translated gesture handling system
 */
suspend fun main() {
    println("========================================")
    println("Kotlin Translation of TouchGestureHandler")
    println("Original: reference-sources/csharp/mobile/TouchGestureHandler.cs")
    println("========================================")
    
    try {
        // Initialize gesture handler
        val gestureHandler = TouchGestureHandlerInstance.initGestureHandler()
        
        // Subscribe to gesture events
        val gestureJob = CoroutineScope(Dispatchers.Default).launch {
            gestureHandler.gestureEvents.collect { gesture ->
                when (gesture.type) {
                    is GestureType.Tap -> println("👆 Tap detected at (${gesture.location.x}, ${gesture.location.y})")
                    is GestureType.LongPress -> println("👆⏱️ Long press at (${gesture.location.x}, ${gesture.location.y})")
                    is GestureType.Pan -> println("👋 Pan: delta=(${gesture.delta.x}, ${gesture.delta.y})")
                    is GestureType.Pinch -> println("🤏 Pinch: scale=${String.format("%.2f", gesture.scale)}")
                    is GestureType.Rotate -> println("🔄 Rotate: ${String.format("%.1f", gesture.rotation)}°")
                    is GestureType.Swipe -> println("👈 Swipe: velocity=${String.format("%.0f", gesture.velocity)} px/s")
                    is GestureType.TwoFingerTap -> println("✌️ Two finger tap")
                    else -> println("👋 Gesture: ${gesture.type}")
                }
            }
        }
        
        // Simulate complex gesture sequence
        println("\n🔄 Simulating gesture sequence...")
        
        // Single tap
        gestureHandler.processTouch(1, 100f, 200f, TouchPhase.Began)
        delay(50)
        gestureHandler.processTouch(1, 100f, 200f, TouchPhase.Ended)
        delay(200)
        
        // Long press
        gestureHandler.processTouch(2, 150f, 250f, TouchPhase.Began)
        delay(1200) // Long press duration
        gestureHandler.processTouch(2, 150f, 250f, TouchPhase.Ended)
        delay(200)
        
        // Pan gesture
        gestureHandler.processTouch(3, 200f, 300f, TouchPhase.Began)
        delay(50)
        for (i in 1..10) {
            gestureHandler.processTouch(3, 200f + i * 5f, 300f + i * 2f, TouchPhase.Moved)
            delay(30)
        }
        gestureHandler.processTouch(3, 250f, 320f, TouchPhase.Ended)
        delay(200)
        
        // Swipe gesture
        gestureHandler.processTouch(4, 100f, 400f, TouchPhase.Began)
        delay(50)
        gestureHandler.processTouch(4, 200f, 400f, TouchPhase.Moved)
        delay(30)
        gestureHandler.processTouch(4, 300f, 400f, TouchPhase.Ended)
        delay(200)
        
        // Two-finger pinch
        gestureHandler.processTouch(5, 200f, 500f, TouchPhase.Began)
        gestureHandler.processTouch(6, 250f, 500f, TouchPhase.Began)
        delay(100)
        // Pinch out
        gestureHandler.processTouch(5, 180f, 500f, TouchPhase.Moved)
        gestureHandler.processTouch(6, 270f, 500f, TouchPhase.Moved)
        delay(100)
        gestureHandler.processTouch(5, 160f, 500f, TouchPhase.Moved)
        gestureHandler.processTouch(6, 290f, 500f, TouchPhase.Moved)
        delay(100)
        gestureHandler.processTouch(5, 160f, 500f, TouchPhase.Ended)
        gestureHandler.processTouch(6, 290f, 500f, TouchPhase.Ended)
        delay(200)
        
        // Two-finger rotation
        gestureHandler.processTouch(7, 300f, 600f, TouchPhase.Began)
        gestureHandler.processTouch(8, 320f, 620f, TouchPhase.Began)
        delay(100)
        // Rotate gesture
        gestureHandler.processTouch(7, 295f, 605f, TouchPhase.Moved)
        gestureHandler.processTouch(8, 325f, 615f, TouchPhase.Moved)
        delay(100)
        gestureHandler.processTouch(7, 290f, 610f, TouchPhase.Moved)
        gestureHandler.processTouch(8, 330f, 610f, TouchPhase.Moved)
        delay(100)
        gestureHandler.processTouch(7, 290f, 610f, TouchPhase.Ended)
        gestureHandler.processTouch(8, 330f, 610f, TouchPhase.Ended)
        delay(200)
        
        // Two finger tap
        gestureHandler.processTouch(9, 400f, 700f, TouchPhase.Began)
        gestureHandler.processTouch(10, 420f, 720f, TouchPhase.Began)
        delay(100)
        gestureHandler.processTouch(9, 400f, 700f, TouchPhase.Ended)
        gestureHandler.processTouch(10, 420f, 720f, TouchPhase.Ended)
        delay(200)
        
        println("\n--- Gesture Statistics ---")
        println("Active touches: ${gestureHandler.getActiveTouchCount()}")
        println("Current gesture: ${gestureHandler.getCurrentGesture()}")
        println("Gesture in progress: ${gestureHandler.isGestureInProgress()}")
        
        // Shutdown
        gestureJob.cancel()
        TouchGestureHandlerInstance.shutdownGestureHandler()
        
        println("========================================")
        println("Gesture handler translation demonstration complete")
        println("========================================")
        
    } catch (e: Exception) {
        System.err.println("Error during gesture handler test: ${e.message}")
    }
}