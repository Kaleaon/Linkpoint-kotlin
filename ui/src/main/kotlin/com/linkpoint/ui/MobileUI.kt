package com.linkpoint.ui

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Mobile UI Components
 * 
 * Mobile-optimized UI components inspired by Lumiya Viewer's touch interface.
 * These components are designed for finger-friendly interaction with large
 * touch targets, gesture-based navigation, and efficient use of screen space.
 */

/**
 * Mobile Chat UI - Slide-up panel with touch optimization
 * 
 * Provides a mobile-friendly chat interface with:
 * - Slide-up panel that can be dismissed with a swipe
 * - Large, touch-friendly message composition area
 * - Channel switching via horizontal swipe
 * - Auto-hide functionality to maximize world view
 */
class MobileChatUI(private val isPhone: Boolean) : UIComponent() {
    
    private var isVisible = false
    private var currentChannel = "Local"
    private val messages = mutableListOf<ChatMessage>()
    private val _newMessage = MutableSharedFlow<ChatMessage>()
    val newMessage = _newMessage.asSharedFlow()
    
    override suspend fun applyTheme(theme: UITheme) {
        val colors = when (theme) {
            UITheme.LIGHT -> ChatColors(
                background = "#FFFFFF",
                text = "#000000",
                accent = "#007AFF"
            )
            UITheme.DARK -> ChatColors(
                background = "#1C1C1C",
                text = "#FFFFFF", 
                accent = "#007AFF"
            )
            UITheme.HIGH_CONTRAST -> ChatColors(
                background = "#000000",
                text = "#FFFF00",
                accent = "#00FF00"
            )
            UITheme.CUSTOM -> ChatColors(
                background = "#2D2D30",
                text = "#CCCCCC",
                accent = "#569CD6"
            )
        }
        
        println("MobileChatUI: Applied ${theme.name} theme")
    }
    
    override suspend fun show() {
        isVisible = true
        println("MobileChatUI: Showing chat panel")
        
        // Simulate slide-up animation
        simulateSlideUpAnimation()
    }
    
    override suspend fun hide() {
        isVisible = false
        println("MobileChatUI: Hiding chat panel")
        
        // Simulate slide-down animation
        simulateSlideDownAnimation()
    }
    
    override suspend fun updateLayout(screenSize: ScreenSize) {
        val panelHeight = if (isPhone) {
            // On phones, chat takes up more screen space when visible
            (screenSize.height * 0.6).toInt()
        } else {
            // On tablets, chat can be smaller relative to screen
            (screenSize.height * 0.4).toInt()
        }
        
        println("MobileChatUI: Updated layout for ${screenSize.width}x${screenSize.height}")
        println("Chat panel height: $panelHeight")
    }
    
    /**
     * Send a chat message
     */
    suspend fun sendMessage(message: String, channel: String = currentChannel) {
        val chatMessage = ChatMessage(
            text = message,
            channel = channel,
            timestamp = System.currentTimeMillis(),
            sender = "LocalUser"
        )
        
        messages.add(chatMessage)
        _newMessage.emit(chatMessage)
        
        println("MobileChatUI: Sent message to $channel: $message")
    }
    
    /**
     * Switch chat channel
     */
    suspend fun switchChannel(channel: String) {
        currentChannel = channel
        println("MobileChatUI: Switched to channel: $channel")
    }
    
    /**
     * Handle incoming chat message
     */
    suspend fun receiveMessage(message: ChatMessage) {
        messages.add(message)
        _newMessage.emit(message)
        
        // Auto-show chat panel for new messages if hidden
        if (!isVisible) {
            show()
            delay(3000) // Auto-hide after 3 seconds
            hide()
        }
    }
    
    private suspend fun simulateSlideUpAnimation() {
        println("MobileChatUI: Animating slide-up...")
        delay(300) // Simulate 300ms animation
        println("MobileChatUI: Slide-up animation complete")
    }
    
    private suspend fun simulateSlideDownAnimation() {
        println("MobileChatUI: Animating slide-down...")
        delay(300) // Simulate 300ms animation
        println("MobileChatUI: Slide-down animation complete")
    }
}

/**
 * Mobile Inventory UI - Grid-based browser optimized for touch
 * 
 * Provides a mobile-friendly inventory interface with:
 * - Grid layout optimized for finger navigation
 * - Category-based browsing with large, clear icons
 * - Drag-and-drop with haptic feedback
 * - Search functionality with voice input support
 */
class MobileInventoryUI(private val isPhone: Boolean) : UIComponent() {
    
    private var isVisible = false
    private var currentCategory = "All"
    private val items = mutableMapOf<String, InventoryItem>()
    
    init {
        // Initialize with sample inventory items
        initializeSampleInventory()
    }
    
    override suspend fun applyTheme(theme: UITheme) {
        println("MobileInventoryUI: Applied ${theme.name} theme")
    }
    
    override suspend fun show() {
        isVisible = true
        println("MobileInventoryUI: Showing inventory grid")
    }
    
    override suspend fun hide() {
        isVisible = false
        println("MobileInventoryUI: Hiding inventory grid")
    }
    
    override suspend fun updateLayout(screenSize: ScreenSize) {
        val gridColumns = if (isPhone) {
            if (screenSize.isPortrait) 3 else 5
        } else {
            if (screenSize.isPortrait) 4 else 6
        }
        
        println("MobileInventoryUI: Updated grid layout: $gridColumns columns")
    }
    
    /**
     * Filter inventory by category
     */
    suspend fun filterByCategory(category: String) {
        currentCategory = category
        val filteredCount = items.values.count { 
            category == "All" || it.category == category 
        }
        
        println("MobileInventoryUI: Filtered to $category category ($filteredCount items)")
    }
    
    /**
     * Search inventory items
     */
    suspend fun searchItems(query: String): List<InventoryItem> {
        val results = items.values.filter { item ->
            item.name.contains(query, ignoreCase = true) ||
            item.description.contains(query, ignoreCase = true)
        }
        
        println("MobileInventoryUI: Search '$query' returned ${results.size} results")
        return results
    }
    
    /**
     * Wear/attach an item
     */
    suspend fun wearItem(itemId: String) {
        val item = items[itemId]
        if (item != null) {
            println("MobileInventoryUI: Wearing item: ${item.name}")
            // Simulate haptic feedback
            simulateHapticFeedback()
        }
    }
    
    private fun initializeSampleInventory() {
        items["shirt1"] = InventoryItem("Blue Shirt", "Clothing", "A comfortable blue shirt")
        items["pants1"] = InventoryItem("Black Pants", "Clothing", "Stylish black pants")
        items["hair1"] = InventoryItem("Blonde Hair", "Body Parts", "Long blonde hair")
        items["sword1"] = InventoryItem("Magic Sword", "Objects", "A mystical glowing sword")
        items["dance1"] = InventoryItem("Dance Animation", "Animations", "Smooth dance moves")
    }
    
    private suspend fun simulateHapticFeedback() {
        println("MobileInventoryUI: *haptic feedback*")
        delay(50)
    }
}

/**
 * Mobile Camera UI - Gesture-based camera controls
 * 
 * Provides touch-optimized camera controls with:
 * - Pan, zoom, and rotate gestures
 * - Preset camera angles with quick access
 * - Smooth transitions with momentum
 * - Integration with device orientation
 */
class MobileCameraUI(private val isPhone: Boolean) : UIComponent() {
    
    private var cameraMode = CameraMode.THIRD_PERSON
    private var zoom = 1.0f
    private var rotation = 0.0f
    private var tilt = 0.0f
    
    override suspend fun applyTheme(theme: UITheme) {
        println("MobileCameraUI: Applied ${theme.name} theme")
    }
    
    override suspend fun show() {
        println("MobileCameraUI: Camera controls active")
    }
    
    override suspend fun hide() {
        println("MobileCameraUI: Camera controls hidden")
    }
    
    override suspend fun updateLayout(screenSize: ScreenSize) {
        println("MobileCameraUI: Updated for ${screenSize.width}x${screenSize.height}")
    }
    
    /**
     * Handle pan gesture
     */
    suspend fun handlePanGesture(deltaX: Float, deltaY: Float) {
        rotation += deltaX * 0.01f
        tilt += deltaY * 0.01f
        
        // Clamp tilt to reasonable values
        tilt = tilt.coerceIn(-1.5f, 1.5f)
        
        println("MobileCameraUI: Pan gesture - rotation: $rotation, tilt: $tilt")
    }
    
    /**
     * Handle zoom gesture
     */
    suspend fun handleZoomGesture(scaleFactor: Float) {
        zoom *= scaleFactor
        zoom = zoom.coerceIn(0.1f, 10.0f)
        
        println("MobileCameraUI: Zoom gesture - zoom: $zoom")
    }
    
    /**
     * Set camera mode
     */
    suspend fun setCameraMode(mode: CameraMode) {
        cameraMode = mode
        println("MobileCameraUI: Changed camera mode to $mode")
        
        // Apply mode-specific settings
        when (mode) {
            CameraMode.FIRST_PERSON -> {
                zoom = 1.0f
                println("MobileCameraUI: First person view activated")
            }
            CameraMode.THIRD_PERSON -> {
                zoom = 3.0f
                println("MobileCameraUI: Third person view activated")
            }
            CameraMode.FREE_CAMERA -> {
                println("MobileCameraUI: Free camera mode activated")
            }
        }
    }
    
    /**
     * Reset camera to default position
     */
    suspend fun resetCamera() {
        zoom = 3.0f
        rotation = 0.0f
        tilt = 0.0f
        cameraMode = CameraMode.THIRD_PERSON
        
        println("MobileCameraUI: Camera reset to defaults")
    }
}

/**
 * Mobile World Map UI - Full-screen map with touch navigation
 */
class MobileWorldMapUI(private val isPhone: Boolean) : UIComponent() {
    
    private var isVisible = false
    private var mapZoom = 1.0f
    private var mapCenterX = 128.0f
    private var mapCenterY = 128.0f
    
    override suspend fun applyTheme(theme: UITheme) {
        println("MobileWorldMapUI: Applied ${theme.name} theme")
    }
    
    override suspend fun show() {
        isVisible = true
        println("MobileWorldMapUI: Showing full-screen world map")
    }
    
    override suspend fun hide() {
        isVisible = false
        println("MobileWorldMapUI: Hiding world map")
    }
    
    override suspend fun updateLayout(screenSize: ScreenSize) {
        println("MobileWorldMapUI: Full-screen map ${screenSize.width}x${screenSize.height}")
    }
    
    /**
     * Handle map pan
     */
    suspend fun panMap(deltaX: Float, deltaY: Float) {
        mapCenterX += deltaX
        mapCenterY += deltaY
        
        println("MobileWorldMapUI: Map panned to ($mapCenterX, $mapCenterY)")
    }
    
    /**
     * Handle map zoom
     */
    suspend fun zoomMap(scaleFactor: Float) {
        mapZoom *= scaleFactor
        mapZoom = mapZoom.coerceIn(0.1f, 10.0f)
        
        println("MobileWorldMapUI: Map zoomed to $mapZoom")
    }
    
    /**
     * Teleport to location (with confirmation)
     */
    suspend fun teleportTo(x: Float, y: Float) {
        println("MobileWorldMapUI: Teleport requested to ($x, $y)")
        println("MobileWorldMapUI: Showing confirmation dialog...")
        delay(1000) // Simulate user confirmation
        println("MobileWorldMapUI: Teleport confirmed - initiating teleport")
    }
}

/**
 * Mobile Avatar UI - Appearance controls optimized for mobile
 */
class MobileAvatarUI(private val isPhone: Boolean) : UIComponent() {
    
    private var isVisible = false
    private val wornItems = mutableSetOf<String>()
    
    override suspend fun applyTheme(theme: UITheme) {
        println("MobileAvatarUI: Applied ${theme.name} theme")
    }
    
    override suspend fun show() {
        isVisible = true
        println("MobileAvatarUI: Showing avatar appearance controls")
    }
    
    override suspend fun hide() {
        isVisible = false
        println("MobileAvatarUI: Hiding avatar controls")
    }
    
    override suspend fun updateLayout(screenSize: ScreenSize) {
        val layout = if (isPhone && screenSize.isPortrait) {
            "Single column layout"
        } else {
            "Multi-column layout"
        }
        
        println("MobileAvatarUI: Using $layout")
    }
    
    /**
     * Wear an outfit
     */
    suspend fun wearOutfit(outfitName: String) {
        println("MobileAvatarUI: Wearing outfit: $outfitName")
        wornItems.clear()
        wornItems.addAll(listOf("shirt", "pants", "shoes", "hair"))
        println("MobileAvatarUI: Outfit applied with ${wornItems.size} items")
    }
    
    /**
     * Detach all attachments
     */
    suspend fun detachAll() {
        val detachedCount = wornItems.size
        wornItems.clear()
        println("MobileAvatarUI: Detached $detachedCount items")
    }
}

/**
 * Mobile Layout Manager - Handles mobile-specific layout logic
 */
class MobileLayoutManager(private val isPhone: Boolean) : LayoutManager() {
    
    override suspend fun updateScreenSize(width: Int, height: Int) {
        val orientation = if (width > height) "landscape" else "portrait"
        val deviceType = if (isPhone) "phone" else "tablet"
        
        println("MobileLayoutManager: $deviceType in $orientation (${width}x${height})")
    }
    
    override suspend fun arrangeComponents(components: Map<String, UIComponent>) {
        println("MobileLayoutManager: Arranging ${components.size} components for mobile")
        
        components.forEach { (name, component) ->
            // Mobile components are typically full-screen or overlay-based
            println("MobileLayoutManager: Positioning $name component")
        }
    }
}

// Data classes for mobile UI

data class ChatMessage(
    val text: String,
    val channel: String,
    val timestamp: Long,
    val sender: String
)

data class ChatColors(
    val background: String,
    val text: String,
    val accent: String
)

data class InventoryItem(
    val name: String,
    val category: String,
    val description: String
)

enum class CameraMode {
    FIRST_PERSON,
    THIRD_PERSON,
    FREE_CAMERA
}