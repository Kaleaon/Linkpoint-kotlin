package com.linkpoint.android.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Binder
import kotlinx.coroutines.*
import com.linkpoint.core.SimpleViewerCore
import com.linkpoint.protocol.LoginSystem
import com.linkpoint.audio.AudioSystem
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Background service for virtual world viewer operations
 * 
 * Handles long-running viewer operations that need to continue
 * when the app is in the background, such as maintaining
 * connections to virtual world simulators.
 */
class ViewerService : Service() {
    
    private val binder = ViewerBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Core viewer systems
    private val viewerCore = SimpleViewerCore()
    private val loginSystem = LoginSystem()
    private val audioSystem = AudioSystem()
    
    inner class ViewerBinder : Binder() {
        fun getService(): ViewerService = this@ViewerService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        logger.info { "ViewerService created" }
        
        serviceScope.launch {
            initializeViewer()
        }
    }
    
    private suspend fun initializeViewer() {
        try {
            logger.info { "Initializing viewer systems in background service..." }
            
            viewerCore.initialize()
            audioSystem.initialize()
            
            logger.info { "Viewer service initialization complete" }
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to initialize viewer service" }
        }
    }
    
    fun getViewerCore() = viewerCore
    fun getLoginSystem() = loginSystem
    fun getAudioSystem() = audioSystem
    
    override fun onDestroy() {
        super.onDestroy()
        logger.info { "ViewerService destroying..." }
        
        serviceScope.launch {
            try {
                viewerCore.shutdown()
                audioSystem.shutdown()
                logger.info { "Viewer service shutdown complete" }
            } catch (e: Exception) {
                logger.error(e) { "Error during service shutdown" }
            } finally {
                serviceScope.cancel()
            }
        }
    }
}