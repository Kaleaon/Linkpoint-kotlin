package com.linkpoint.ui

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Multi-Platform UI Framework
 * 
 * Core framework for managing UI across mobile and desktop platforms.
 * Inspired by Lumiya Viewer's mobile interface innovations while maintaining
 * desktop virtual world viewer compatibility.
 * 
 * This framework automatically detects platform capabilities and adapts
 * the interface accordingly, providing touch-optimized controls on mobile
 * and traditional windowed interfaces on desktop.
 */
class UIFramework private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: UIFramework? = null
        
        /**
         * Get the singleton UIFramework instance
         */
        fun getInstance(): UIFramework {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UIFramework().also { INSTANCE = it }
            }
        }
    }
    
    private val _platformType = MutableStateFlow(PlatformType.UNKNOWN)
    val platformType: StateFlow<PlatformType> = _platformType.asStateFlow()
    
    private val _screenSize = MutableStateFlow(ScreenSize(0, 0))
    val screenSize: StateFlow<ScreenSize> = _screenSize.asStateFlow()
    
    private val _theme = MutableStateFlow(UITheme.DARK)
    val theme: StateFlow<UITheme> = _theme.asStateFlow()
    
    private val _components = mutableMapOf<String, UIComponent>()
    private val _layouts = mutableMapOf<String, LayoutManager>()
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    /**
     * Initialize the UI framework with platform detection
     */
    suspend fun initialize(screenWidth: Int, screenHeight: Int): Boolean {
        return try {
            // Detect platform type based on screen size and capabilities
            val detectedPlatform = detectPlatform(screenWidth, screenHeight)
            _platformType.value = detectedPlatform
            _screenSize.value = ScreenSize(screenWidth, screenHeight)
            
            // Initialize platform-specific components
            initializePlatformComponents(detectedPlatform)
            
            println("UIFramework initialized for ${detectedPlatform.name} platform")
            println("Screen size: ${screenWidth}x${screenHeight}")
            
            true
        } catch (e: Exception) {
            println("Failed to initialize UIFramework: ${e.message}")
            false
        }
    }
    
    /**
     * Detect platform type based on screen characteristics
     */
    private fun detectPlatform(width: Int, height: Int): PlatformType {
        return when {
            // Mobile phone - portrait orientation, small screen
            width < 800 && height > width -> PlatformType.MOBILE_PHONE
            // Mobile tablet - larger screen but still touch-oriented
            width < 1200 && (width > 800 || height > 800) -> PlatformType.MOBILE_TABLET
            // Desktop - large screen, landscape orientation
            else -> PlatformType.DESKTOP
        }
    }
    
    /**
     * Initialize platform-specific UI components
     */
    private suspend fun initializePlatformComponents(platform: PlatformType) {
        when (platform) {
            PlatformType.MOBILE_PHONE -> initializeMobileComponents(true)
            PlatformType.MOBILE_TABLET -> initializeMobileComponents(false)
            PlatformType.DESKTOP -> initializeDesktopComponents()
            PlatformType.UNKNOWN -> {
                // Default to desktop components
                initializeDesktopComponents()
            }
        }
    }
    
    /**
     * Initialize mobile-specific UI components
     */
    private suspend fun initializeMobileComponents(isPhone: Boolean) {
        // Chat UI - slide-up panel with touch optimization
        registerComponent("chat", MobileChatUI(isPhone))
        
        // Inventory UI - grid-based browser
        registerComponent("inventory", MobileInventoryUI(isPhone))
        
        // Camera UI - gesture-based controls
        registerComponent("camera", MobileCameraUI(isPhone))
        
        // World Map UI - full-screen with touch navigation
        registerComponent("worldmap", MobileWorldMapUI(isPhone))
        
        // Avatar UI - mobile-optimized appearance controls
        registerComponent("avatar", MobileAvatarUI(isPhone))
        
        // Create mobile layout manager
        _layouts["main"] = MobileLayoutManager(isPhone)
        
        println("Initialized mobile UI components (phone: $isPhone)")
    }
    
    /**
     * Initialize desktop-specific UI components
     */
    private suspend fun initializeDesktopComponents() {
        // Chat UI - traditional windowed interface
        registerComponent("chat", DesktopChatUI())
        
        // Inventory UI - tree-view with detailed controls
        registerComponent("inventory", DesktopInventoryUI())
        
        // Camera UI - mouse and keyboard controls
        registerComponent("camera", DesktopCameraUI())
        
        // World Map UI - resizable window with advanced features
        registerComponent("worldmap", DesktopWorldMapUI())
        
        // Avatar UI - comprehensive appearance editor
        registerComponent("avatar", DesktopAvatarUI())
        
        // Create desktop layout manager
        _layouts["main"] = DesktopLayoutManager()
        
        println("Initialized desktop UI components")
    }
    
    /**
     * Register a UI component with the framework
     */
    fun registerComponent(name: String, component: UIComponent) {
        _components[name] = component
        
        // Initialize the component with current theme
        coroutineScope.launch {
            component.applyTheme(_theme.value)
        }
    }
    
    /**
     * Get a registered UI component
     */
    fun getComponent(name: String): UIComponent? {
        return _components[name]
    }
    
    /**
     * Change the UI theme
     */
    suspend fun setTheme(newTheme: UITheme) {
        _theme.value = newTheme
        
        // Apply theme to all registered components
        _components.values.forEach { component ->
            component.applyTheme(newTheme)
        }
        
        println("Applied theme: ${newTheme.name}")
    }
    
    /**
     * Handle screen size changes (device rotation, window resize)
     */
    suspend fun updateScreenSize(width: Int, height: Int) {
        val oldPlatform = _platformType.value
        val newPlatform = detectPlatform(width, height)
        
        _screenSize.value = ScreenSize(width, height)
        
        // Reinitialize if platform type changed
        if (oldPlatform != newPlatform) {
            _platformType.value = newPlatform
            _components.clear()
            _layouts.clear()
            initializePlatformComponents(newPlatform)
            println("Platform changed from $oldPlatform to $newPlatform")
        } else {
            // Just update layouts
            _layouts["main"]?.updateScreenSize(width, height)
        }
    }
    
    /**
     * Get the current layout manager
     */
    fun getLayoutManager(): LayoutManager? {
        return _layouts["main"]
    }
    
    /**
     * Shutdown the UI framework
     */
    fun shutdown() {
        coroutineScope.cancel()
        _components.clear()
        _layouts.clear()
        println("UIFramework shutdown complete")
    }
}

/**
 * Platform types supported by the UI framework
 */
enum class PlatformType {
    MOBILE_PHONE,    // Small touch screen, portrait orientation
    MOBILE_TABLET,   // Larger touch screen, may be landscape or portrait
    DESKTOP,         // Large screen with mouse and keyboard
    UNKNOWN          // Platform not detected or unsupported
}

/**
 * Screen size information
 */
data class ScreenSize(
    val width: Int,
    val height: Int
) {
    val isLandscape: Boolean = width > height
    val isPortrait: Boolean = height > width
    val aspectRatio: Float = width.toFloat() / height.toFloat()
}

/**
 * UI theme options
 */
enum class UITheme {
    LIGHT,           // Light theme for daytime use
    DARK,            // Dark theme for low-light environments
    HIGH_CONTRAST,   // High contrast theme for accessibility
    CUSTOM           // User-defined custom theme
}

/**
 * Base class for all UI components in the framework
 */
abstract class UIComponent {
    
    /**
     * Apply a theme to this component
     */
    abstract suspend fun applyTheme(theme: UITheme)
    
    /**
     * Show this component
     */
    abstract suspend fun show()
    
    /**
     * Hide this component
     */
    abstract suspend fun hide()
    
    /**
     * Update component layout for screen size changes
     */
    abstract suspend fun updateLayout(screenSize: ScreenSize)
}

/**
 * Base class for layout managers
 */
abstract class LayoutManager {
    
    /**
     * Update layout for screen size changes
     */
    abstract suspend fun updateScreenSize(width: Int, height: Int)
    
    /**
     * Arrange components in the layout
     */
    abstract suspend fun arrangeComponents(components: Map<String, UIComponent>)
}