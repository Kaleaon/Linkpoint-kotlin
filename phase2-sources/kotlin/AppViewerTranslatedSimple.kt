/**
 * Simplified Kotlin Translation of SecondLife Viewer Core Application
 * 
 * Original Source: LLAppViewer from secondlife/viewer repository
 * Original File: indra/newview/llappviewer.cpp
 * 
 * This simplified version demonstrates the core translation patterns without
 * external dependencies for easier compilation and demonstration.
 * 
 * Key Modernizations Applied:
 * - Thread-safe design with atomic operations
 * - Modern event handling patterns
 * - Null safety throughout
 * - Structured error handling
 * - Resource management with proper cleanup
 */

import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

/**
 * Application startup states - sealed class provides type safety
 */
sealed class StartupState {
    object First : StartupState()
    object BrowserInit : StartupState()
    object LoginShow : StartupState()
    object WorldInit : StartupState() 
    object Started : StartupState()
}

/**
 * Application lifecycle events
 */
sealed class AppEvent {
    object InitializationStarted : AppEvent()
    object InitializationCompleted : AppEvent()
    object FrameProcessed : AppEvent()
    object QuitRequested : AppEvent()
    object CleanupStarted : AppEvent()
    object CleanupCompleted : AppEvent()
    data class Error(val message: String, val cause: Throwable? = null) : AppEvent()
}

/**
 * Viewer information data class
 */
data class ViewerInfo(
    val serialNumber: String,
    val frameCount: Long,
    val quitRequested: Boolean,
    val logoutRequestSent: Boolean,
    val isSecondInstance: Boolean,
    val startTime: Instant,
    val serverReleaseNotesUrl: String? = null
)

/**
 * Configuration settings
 */
data class ViewerConfiguration(
    val randomizeFramerate: Boolean = false,
    val periodicSlowFrame: Boolean = false,
    val purgeCache: Boolean = false,
    val targetFps: Int = 60
)

/**
 * Core SecondLife Viewer Application - Simplified Kotlin Translation
 * 
 * Modernized implementation of the original C++ LLAppViewer class with:
 * - Thread-safe concurrent processing
 * - Event-driven architecture
 * - Null-safe design throughout
 * - Structured error handling
 * - Automatic resource management
 */
class AppViewerTranslatedSimple private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: AppViewerTranslatedSimple? = null
        
        /**
         * Thread-safe singleton access
         */
        fun instance(): AppViewerTranslatedSimple {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppViewerTranslatedSimple().also { INSTANCE = it }
            }
        }
    }
    
    // Application state management with thread safety
    private val _quitRequested = AtomicBoolean(false)
    private val _logoutRequestSent = AtomicBoolean(false)
    private val _secondInstance = AtomicBoolean(false)
    private val _savedFinalSnapshot = AtomicBoolean(false)
    private val _frameCount = AtomicLong(0)
    
    // Configuration and metadata
    private val startTime: Instant = Instant.now()
    private val serialNumber: String = generateSerialNumber()
    private var configuration = ViewerConfiguration()
    private var serverReleaseNotesUrl: String? = null
    
    // Event handling
    private val events = ConcurrentLinkedQueue<AppEvent>()
    private val eventListeners = mutableListOf<(AppEvent) -> Unit>()
    
    // Main application loop
    private var mainLoopThread: Thread? = null
    private val shutdownLatch = CountDownLatch(1)
    
    /**
     * Add event listener for monitoring application events
     */
    fun addEventListener(listener: (AppEvent) -> Unit) {
        synchronized(eventListeners) {
            eventListeners.add(listener)
        }
    }
    
    /**
     * Emit an event to all listeners
     */
    private fun emitEvent(event: AppEvent) {
        events.offer(event)
        synchronized(eventListeners) {
            eventListeners.forEach { listener ->
                try {
                    listener(event)
                } catch (e: Exception) {
                    println("Event listener error: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Initialize the viewer application
     * 
     * This method handles the complex initialization sequence including:
     * - System initialization
     * - Network setup
     * - UI preparation
     * - Asset loading
     */
    fun initialize(): Boolean {
        return try {
            emitEvent(AppEvent.InitializationStarted)
            println("AppViewer::initialize() - Starting viewer initialization")
            
            // Initialize components sequentially with proper error handling
            val steps = listOf(
                "Logging System" to ::initializeLoggingSystem,
                "Configuration" to ::initializeConfiguration,
                "Threading" to ::initializeThreading,
                "Cache System" to ::initializeCache,
                "Window System" to ::initializeWindow
            )
            
            for ((stepName, initStep) in steps) {
                try {
                    if (!initStep()) {
                        throw IllegalStateException("Failed to initialize $stepName")
                    }
                    println("✓ $stepName initialized successfully")
                } catch (e: Exception) {
                    emitEvent(AppEvent.Error("Initialization failed at $stepName", e))
                    println("✗ Failed to initialize $stepName: ${e.message}")
                    return false
                }
            }
            
            emitEvent(AppEvent.InitializationCompleted)
            println("AppViewer::initialize() - Initialization complete")
            true
            
        } catch (e: Exception) {
            emitEvent(AppEvent.Error("Initialization failed", e))
            false
        }
    }
    
    /**
     * Start the main application loop
     */
    fun start() {
        mainLoopThread = thread(name = "MainLoop") {
            try {
                mainLoop()
            } catch (e: InterruptedException) {
                println("Main loop interrupted")
            } catch (e: Exception) {
                emitEvent(AppEvent.Error("Main loop error", e))
                println("Main loop error: ${e.message}")
            } finally {
                shutdownLatch.countDown()
            }
        }
    }
    
    /**
     * Process a single frame
     * 
     * Returns false to indicate shutdown should occur
     */
    fun processFrame(): Boolean {
        // Check for quit request
        if (_quitRequested.get()) {
            println("AppViewer::processFrame() - Quit requested, shutting down")
            return false
        }
        
        // Increment frame counter atomically
        val currentFrame = _frameCount.incrementAndGet()
        
        // Execute frame processing
        return try {
            if (!doFrame()) {
                return false
            }
            
            // Handle periodic slow frames for testing
            if (configuration.periodicSlowFrame && (currentFrame % 120L == 0L)) {
                Thread.sleep(100)
            }
            
            emitEvent(AppEvent.FrameProcessed)
            true
            
        } catch (e: Exception) {
            emitEvent(AppEvent.Error("Frame processing error", e))
            false
        }
    }
    
    /**
     * Clean up application resources
     */
    fun cleanup(): Boolean {
        return try {
            emitEvent(AppEvent.CleanupStarted)
            println("AppViewer::cleanup() - Starting shutdown sequence")
            
            // Save final snapshot if needed
            if (!_savedFinalSnapshot.get()) {
                saveFinalSnapshot()
            }
            
            // Stop main loop
            mainLoopThread?.interrupt()
            
            // Wait for main loop to finish
            try {
                shutdownLatch.await()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            
            // Clean up components in reverse order 
            cleanupSavedSettings()
            removeMarkerFiles()
            
            emitEvent(AppEvent.CleanupCompleted)
            println("AppViewer::cleanup() - Shutdown complete")
            true
            
        } catch (e: Exception) {
            emitEvent(AppEvent.Error("Cleanup failed", e))
            false
        }
    }
    
    /**
     * Request graceful quit
     */
    fun requestQuit() {
        println("AppViewer::requestQuit() - Graceful quit requested")
        _quitRequested.set(true)
        emitEvent(AppEvent.QuitRequested)
    }
    
    /**
     * Force immediate quit
     */
    fun forceQuit() {
        println("AppViewer::forceQuit() - Force quit requested")
        _quitRequested.set(true)
        emitEvent(AppEvent.QuitRequested)
        
        // Interrupt main loop immediately
        mainLoopThread?.interrupt()
    }
    
    /**
     * Fast quit with minimal cleanup
     */
    fun fastQuit(errorCode: Int = 0) {
        println("AppViewer::fastQuit() - Fast quit with error code: $errorCode")
        _quitRequested.set(true)
        emitEvent(AppEvent.QuitRequested)
        
        // Force stop without waiting
        mainLoopThread?.interrupt()
    }
    
    /**
     * User-initiated quit
     */
    fun userQuit() {
        println("AppViewer::userQuit() - User quit request")
        // In real implementation, would show confirmation dialog
        requestQuit()
    }
    
    /**
     * Abort a pending quit request
     */
    fun abortQuit() {
        println("AppViewer::abortQuit() - Quit request aborted")
        _quitRequested.set(false)
    }
    
    /**
     * Force disconnection with message
     */
    fun forceDisconnect(message: String) {
        println("AppViewer::forceDisconnect() - $message")
        _logoutRequestSent.set(true)
        requestQuit()
    }
    
    /**
     * Save final snapshot before quit
     */
    private fun saveFinalSnapshot() {
        println("AppViewer::saveFinalSnapshot() - Saving final snapshot")
        _savedFinalSnapshot.set(true)
        // In real implementation, would capture screen
    }
    
    /**
     * Write debug information
     */
    fun writeDebugInfo(isStatic: Boolean = true) {
        println("AppViewer::writeDebugInfo() - Writing debug info (static=$isStatic)")
        // In real implementation, would write system info to file
    }
    
    // Property accessors with thread safety
    val quitRequested: Boolean get() = _quitRequested.get()
    val logoutRequestSent: Boolean get() = _logoutRequestSent.get()
    val isSecondInstance: Boolean get() = _secondInstance.get()
    val frameCount: Long get() = _frameCount.get()
    val hasSavedFinalSnapshot: Boolean get() = _savedFinalSnapshot.get()
    
    /**
     * Get comprehensive viewer information
     */
    fun getViewerInfo(): ViewerInfo {
        return ViewerInfo(
            serialNumber = serialNumber,
            frameCount = frameCount,
            quitRequested = quitRequested,
            logoutRequestSent = logoutRequestSent,
            isSecondInstance = isSecondInstance,
            startTime = startTime,
            serverReleaseNotesUrl = serverReleaseNotesUrl
        )
    }
    
    /**
     * Get viewer information as formatted string
     */
    fun getViewerInfoString(useDefault: Boolean = false): String {
        if (useDefault) {
            return "SecondLife Viewer (Kotlin Translation - Simplified)"
        }
        
        val info = getViewerInfo()
        return buildString {
            appendLine("Viewer Information:")
            appendLine("Serial Number: ${info.serialNumber}")
            appendLine("Frame Count: ${info.frameCount}")
            appendLine("Quit Requested: ${if (info.quitRequested) "Yes" else "No"}")
            appendLine("Start Time: ${info.startTime}")
            info.serverReleaseNotesUrl?.let {
                appendLine("Release Notes URL: $it")
            }
        }
    }
    
    /**
     * Update configuration with thread safety
     */
    @Synchronized
    fun updateConfiguration(newConfig: ViewerConfiguration) {
        configuration = newConfig
        println("Configuration updated: $configuration")
    }
    
    /**
     * Set server release notes URL
     */
    fun setServerReleaseNotesUrl(url: String) {
        serverReleaseNotesUrl = url
    }
    
    // Private implementation methods
    
    /**
     * Main application loop
     */
    private fun mainLoop() {
        println("Main loop started")
        
        try {
            while (!_quitRequested.get() && !Thread.currentThread().isInterrupted) {
                // Calculate frame delay for target FPS
                val frameDelay = 1000L / configuration.targetFps
                
                if (!processFrame()) {
                    break
                }
                
                Thread.sleep(frameDelay)
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            println("Main loop interrupted")
        } finally {
            println("Main loop terminated")
        }
    }
    
    /**
     * Process a single frame (mock implementation)
     */
    private fun doFrame(): Boolean {
        // Mock frame processing
        Thread.sleep(1) // Simulate work
        return true
    }
    
    /**
     * Initialize logging system
     */
    private fun initializeLoggingSystem(): Boolean {
        println("Initializing logging system...")
        Thread.sleep(10) // Simulate initialization time
        return true
    }
    
    /**
     * Initialize configuration system
     */
    private fun initializeConfiguration(): Boolean {
        println("Initializing configuration...")
        Thread.sleep(10)
        return true
    }
    
    /**
     * Initialize threading system
     */
    private fun initializeThreading(): Boolean {
        println("Initializing threading...")
        // Threading handled by thread creation
        return true
    }
    
    /**
     * Initialize cache system
     */
    private fun initializeCache(): Boolean {
        println("Initializing cache...")
        Thread.sleep(20)
        return true
    }
    
    /**
     * Initialize window system
     */
    private fun initializeWindow(): Boolean {
        println("Initializing window...")
        Thread.sleep(30)
        return true
    }
    
    /**
     * Clean up saved settings
     */
    private fun cleanupSavedSettings() {
        println("Cleaning up saved settings...")
        Thread.sleep(10)
    }
    
    /**
     * Remove application marker files
     */
    private fun removeMarkerFiles() {
        println("Removing marker files...")
        Thread.sleep(5)
    }
    
    /**
     * Generate unique serial number
     */
    private fun generateSerialNumber(): String {
        return "SN${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(8)}"
    }
}

/**
 * Demo function showing the modernized Kotlin application lifecycle
 */
fun main() {
    println("=== SecondLife Viewer Core Application (Kotlin Translation - Simplified) ===")
    println("Modernized from LLAppViewer (secondlife/viewer repository)")
    println()
    
    val app = AppViewerTranslatedSimple.instance()
    
    // Set up event monitoring
    app.addEventListener { event ->
        when (event) {
            is AppEvent.InitializationStarted -> println("🔄 Initialization started")
            is AppEvent.InitializationCompleted -> println("✅ Initialization completed")
            is AppEvent.FrameProcessed -> { /* Too verbose to log every frame */ }
            is AppEvent.QuitRequested -> println("🛑 Quit requested")
            is AppEvent.CleanupStarted -> println("🧹 Cleanup started")
            is AppEvent.CleanupCompleted -> println("✅ Cleanup completed")
            is AppEvent.Error -> println("❌ Error: ${event.message}")
        }
    }
    
    try {
        // Initialize application
        if (!app.initialize()) {
            println("❌ Failed to initialize application")
            return
        }
        
        // Start main loop
        app.start()
        
        // Run for demo period
        println("\n🏃 Running application for 3 seconds...")
        
        repeat(3) { i ->
            Thread.sleep(1000)
            println("Frame processing... (second ${i + 1})")
        }
        
        // Show viewer info
        println("\n📊 " + app.getViewerInfoString())
        
        // Test configuration update
        println("\n⚙️ Testing configuration update...")
        app.updateConfiguration(
            ViewerConfiguration(
                randomizeFramerate = true,
                periodicSlowFrame = false,
                targetFps = 30
            )
        )
        
        // Test quit functionality
        println("\n🛑 Testing quit functionality...")
        app.requestQuit()
        
        // Wait a bit for cleanup
        Thread.sleep(500)
        
        // Cleanup
        if (app.cleanup()) {
            println("\n✅ Demo completed successfully!")
        } else {
            println("\n❌ Demo cleanup failed")
        }
        
    } catch (e: Exception) {
        println("❌ Demo error: ${e.message}")
        e.printStackTrace()
    }
}