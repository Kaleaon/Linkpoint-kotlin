package com.linkpoint.ui

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Desktop UI Components
 * 
 * Traditional windowed UI components that maintain compatibility with
 * desktop virtual world viewer patterns while modernizing the underlying
 * architecture. These components support mouse/keyboard interaction,
 * multi-window layouts, and advanced features expected by desktop users.
 */

/**
 * Desktop Chat UI - Traditional windowed chat interface
 * 
 * Provides a desktop-style chat interface with:
 * - Resizable chat window with transparency options
 * - Multiple chat tabs for different channels
 * - Chat history with search and filtering
 * - Command auto-complete and chat logging
 */
class DesktopChatUI : UIComponent() {
    
    private var isVisible = false
    private val chatTabs = mutableMapOf<String, ChatTab>()
    private var activeTab = "Local"
    
    init {
        // Initialize default chat tabs
        chatTabs["Local"] = ChatTab("Local", mutableListOf())
        chatTabs["IM"] = ChatTab("IM", mutableListOf())
        chatTabs["Group"] = ChatTab("Group", mutableListOf())
        chatTabs["System"] = ChatTab("System", mutableListOf())
    }
    
    override suspend fun applyTheme(theme: UITheme) {
        val windowStyle = when (theme) {
            UITheme.LIGHT -> WindowStyle(
                background = "#F0F0F0",
                border = "#CCCCCC",
                text = "#000000",
                accent = "#0078D4"
            )
            UITheme.DARK -> WindowStyle(
                background = "#2D2D30",
                border = "#3E3E42",
                text = "#FFFFFF",
                accent = "#0E639C"
            )
            UITheme.HIGH_CONTRAST -> WindowStyle(
                background = "#000000",
                border = "#FFFFFF",
                text = "#FFFFFF",
                accent = "#FFFF00"
            )
            UITheme.CUSTOM -> WindowStyle(
                background = "#1E1E1E",
                border = "#007ACC",
                text = "#CCCCCC",
                accent = "#569CD6"
            )
        }
        
        println("DesktopChatUI: Applied ${theme.name} theme with window styling")
    }
    
    override suspend fun show() {
        isVisible = true
        println("DesktopChatUI: Showing chat window")
        displayChatWindow()
    }
    
    override suspend fun hide() {
        isVisible = false
        println("DesktopChatUI: Hiding chat window")
    }
    
    override suspend fun updateLayout(screenSize: ScreenSize) {
        val windowWidth = (screenSize.width * 0.3).toInt().coerceAtLeast(300)
        val windowHeight = (screenSize.height * 0.4).toInt().coerceAtLeast(200)
        
        println("DesktopChatUI: Resized chat window to ${windowWidth}x${windowHeight}")
    }
    
    /**
     * Add a new chat tab
     */
    suspend fun addChatTab(tabName: String) {
        if (!chatTabs.containsKey(tabName)) {
            chatTabs[tabName] = ChatTab(tabName, mutableListOf())
            println("DesktopChatUI: Added new chat tab: $tabName")
        }
    }
    
    /**
     * Switch to a different chat tab
     */
    suspend fun switchToTab(tabName: String) {
        if (chatTabs.containsKey(tabName)) {
            activeTab = tabName
            println("DesktopChatUI: Switched to tab: $tabName")
            displayChatTab(tabName)
        }
    }
    
    /**
     * Send message in active tab
     */
    suspend fun sendMessage(message: String) {
        val tab = chatTabs[activeTab]
        if (tab != null) {
            val chatMessage = ChatMessage(
                text = message,
                channel = activeTab,
                timestamp = System.currentTimeMillis(),
                sender = "LocalUser"
            )
            
            tab.messages.add(chatMessage)
            displayMessage(chatMessage)
            
            println("DesktopChatUI: Sent message in $activeTab: $message")
        }
    }
    
    /**
     * Search chat history
     */
    suspend fun searchHistory(query: String): List<ChatMessage> {
        val results = mutableListOf<ChatMessage>()
        
        chatTabs.values.forEach { tab ->
            results.addAll(tab.messages.filter { message ->
                message.text.contains(query, ignoreCase = true) ||
                message.sender.contains(query, ignoreCase = true)
            })
        }
        
        println("DesktopChatUI: Search '$query' returned ${results.size} results")
        return results
    }
    
    private suspend fun displayChatWindow() {
        println("DesktopChatUI: ┌─────────────────────────────────┐")
        println("DesktopChatUI: │ Chat - $activeTab                │")
        println("DesktopChatUI: ├─────────────────────────────────┤")
        
        // Display tabs
        val tabsDisplay = chatTabs.keys.joinToString(" | ") { tabName ->
            if (tabName == activeTab) "[$tabName]" else tabName
        }
        println("DesktopChatUI: │ Tabs: $tabsDisplay")
        println("DesktopChatUI: ├─────────────────────────────────┤")
        
        // Display recent messages
        displayChatTab(activeTab)
        
        println("DesktopChatUI: └─────────────────────────────────┘")
    }
    
    private suspend fun displayChatTab(tabName: String) {
        val tab = chatTabs[tabName]
        if (tab != null) {
            val recentMessages = tab.messages.takeLast(5)
            recentMessages.forEach { message ->
                displayMessage(message)
            }
        }
    }
    
    private fun displayMessage(message: ChatMessage) {
        val timeStr = java.text.SimpleDateFormat("HH:mm").format(message.timestamp)
        println("DesktopChatUI: │ [$timeStr] ${message.sender}: ${message.text}")
    }
}

/**
 * Desktop Inventory UI - Tree-view with detailed controls
 * 
 * Provides a traditional desktop inventory interface with:
 * - Hierarchical folder structure
 * - Multi-select operations with keyboard shortcuts
 * - Detailed list view with sortable columns
 * - Advanced filtering and search capabilities
 */
class DesktopInventoryUI : UIComponent() {
    
    private var isVisible = false
    private val inventoryTree = InventoryFolder("My Inventory", mutableListOf())
    private var sortBy = SortField.NAME
    private var sortAscending = true
    
    init {
        initializeInventoryStructure()
    }
    
    override suspend fun applyTheme(theme: UITheme) {
        println("DesktopInventoryUI: Applied ${theme.name} theme to tree view")
    }
    
    override suspend fun show() {
        isVisible = true
        println("DesktopInventoryUI: Showing inventory window")
        displayInventoryWindow()
    }
    
    override suspend fun hide() {
        isVisible = false
        println("DesktopInventoryUI: Hiding inventory window")
    }
    
    override suspend fun updateLayout(screenSize: ScreenSize) {
        val windowWidth = (screenSize.width * 0.25).toInt().coerceAtLeast(250)
        val windowHeight = (screenSize.height * 0.6).toInt().coerceAtLeast(400)
        
        println("DesktopInventoryUI: Resized inventory window to ${windowWidth}x${windowHeight}")
    }
    
    /**
     * Sort inventory by field
     */
    suspend fun sortInventory(field: SortField, ascending: Boolean = true) {
        sortBy = field
        sortAscending = ascending
        
        println("DesktopInventoryUI: Sorted by ${field.name} (${if (ascending) "ascending" else "descending"})")
        displayInventoryWindow()
    }
    
    /**
     * Search inventory items
     */
    suspend fun searchInventory(query: String): List<InventoryItem> {
        val results = mutableListOf<InventoryItem>()
        searchInFolder(inventoryTree, query, results)
        
        println("DesktopInventoryUI: Search '$query' found ${results.size} items")
        return results
    }
    
    /**
     * Wear multiple selected items
     */
    suspend fun wearSelectedItems(itemIds: List<String>) {
        println("DesktopInventoryUI: Wearing ${itemIds.size} selected items:")
        itemIds.forEach { itemId ->
            println("DesktopInventoryUI: - Wearing item: $itemId")
        }
    }
    
    /**
     * Create new folder
     */
    suspend fun createFolder(parentPath: String, folderName: String) {
        println("DesktopInventoryUI: Created folder '$folderName' in '$parentPath'")
    }
    
    private fun initializeInventoryStructure() {
        // Create folder structure
        val clothingFolder = InventoryFolder("Clothing", mutableListOf())
        clothingFolder.children.add(InventoryItem("Blue Shirt", "Clothing", "A comfortable blue shirt"))
        clothingFolder.children.add(InventoryItem("Black Pants", "Clothing", "Stylish black pants"))
        clothingFolder.children.add(InventoryItem("Red Dress", "Clothing", "Elegant red dress"))
        
        val bodyPartsFolder = InventoryFolder("Body Parts", mutableListOf())
        bodyPartsFolder.children.add(InventoryItem("Hair - Blonde", "Body Parts", "Long blonde hair"))
        bodyPartsFolder.children.add(InventoryItem("Eyes - Blue", "Body Parts", "Bright blue eyes"))
        bodyPartsFolder.children.add(InventoryItem("Skin - Fair", "Body Parts", "Fair skin tone"))
        
        val objectsFolder = InventoryFolder("Objects", mutableListOf())
        objectsFolder.children.add(InventoryItem("Magic Sword", "Objects", "A mystical glowing sword"))
        objectsFolder.children.add(InventoryItem("Wooden Chair", "Objects", "Simple wooden chair"))
        
        val animationsFolder = InventoryFolder("Animations", mutableListOf())
        animationsFolder.children.add(InventoryItem("Dance - Salsa", "Animations", "Passionate salsa dance"))
        animationsFolder.children.add(InventoryItem("Walk - Confident", "Animations", "Confident walking style"))
        
        inventoryTree.children.addAll(listOf(clothingFolder, bodyPartsFolder, objectsFolder, animationsFolder))
    }
    
    private suspend fun displayInventoryWindow() {
        println("DesktopInventoryUI: ┌─────────────────────────────────────┐")
        println("DesktopInventoryUI: │ Inventory                           │")
        println("DesktopInventoryUI: ├─────────────────────────────────────┤")
        println("DesktopInventoryUI: │ Sort: ${sortBy.name} ${if (sortAscending) "↑" else "↓"}              │")
        println("DesktopInventoryUI: ├─────────────────────────────────────┤")
        
        displayFolderContents(inventoryTree, 0)
        
        println("DesktopInventoryUI: └─────────────────────────────────────┘")
    }
    
    private fun displayFolderContents(folder: InventoryFolder, indent: Int) {
        val prefix = "│ " + "  ".repeat(indent)
        println("DesktopInventoryUI: $prefix📁 ${folder.name}")
        
        folder.children.forEach { item ->
            when (item) {
                is InventoryFolder -> displayFolderContents(item, indent + 1)
                is InventoryItem -> {
                    val itemPrefix = "│ " + "  ".repeat(indent + 1)
                    val icon = when (item.category) {
                        "Clothing" -> "👕"
                        "Body Parts" -> "👤"
                        "Objects" -> "📦"
                        "Animations" -> "💃"
                        else -> "📄"
                    }
                    println("DesktopInventoryUI: $itemPrefix$icon ${item.name}")
                }
            }
        }
    }
    
    private fun searchInFolder(folder: InventoryFolder, query: String, results: MutableList<InventoryItem>) {
        folder.children.forEach { item ->
            when (item) {
                is InventoryFolder -> searchInFolder(item, query, results)
                is InventoryItem -> {
                    if (item.name.contains(query, ignoreCase = true) || 
                        item.description.contains(query, ignoreCase = true)) {
                        results.add(item)
                    }
                }
            }
        }
    }
}

/**
 * Desktop Camera UI - Mouse and keyboard camera controls
 */
class DesktopCameraUI : UIComponent() {
    
    private var cameraMode = CameraMode.THIRD_PERSON
    private var mouseSensitivity = 1.0f
    private var keyboardSpeed = 1.0f
    
    override suspend fun applyTheme(theme: UITheme) {
        println("DesktopCameraUI: Applied ${theme.name} theme to camera controls")
    }
    
    override suspend fun show() {
        println("DesktopCameraUI: Desktop camera controls active")
        displayCameraControls()
    }
    
    override suspend fun hide() {
        println("DesktopCameraUI: Desktop camera controls hidden")
    }
    
    override suspend fun updateLayout(screenSize: ScreenSize) {
        println("DesktopCameraUI: Updated for ${screenSize.width}x${screenSize.height}")
    }
    
    /**
     * Handle mouse movement for camera control
     */
    suspend fun handleMouseMovement(deltaX: Float, deltaY: Float, mouseButton: MouseButton) {
        when (mouseButton) {
            MouseButton.LEFT -> {
                println("DesktopCameraUI: Left-click camera rotate: ($deltaX, $deltaY)")
            }
            MouseButton.RIGHT -> {
                println("DesktopCameraUI: Right-click camera pan: ($deltaX, $deltaY)")
            }
            MouseButton.MIDDLE -> {
                println("DesktopCameraUI: Middle-click camera zoom: $deltaY")
            }
            MouseButton.NONE -> {
                // Mouse look mode
                println("DesktopCameraUI: Mouse look: ($deltaX, $deltaY)")
            }
        }
    }
    
    /**
     * Handle keyboard input for camera movement
     */
    suspend fun handleKeyboardInput(key: String, pressed: Boolean) {
        if (pressed) {
            when (key.uppercase()) {
                "W" -> println("DesktopCameraUI: Move forward")
                "S" -> println("DesktopCameraUI: Move backward")
                "A" -> println("DesktopCameraUI: Move left")
                "D" -> println("DesktopCameraUI: Move right")
                "Q" -> println("DesktopCameraUI: Move up")
                "E" -> println("DesktopCameraUI: Move down")
                "F1" -> setCameraMode(CameraMode.FIRST_PERSON)
                "F2" -> setCameraMode(CameraMode.THIRD_PERSON)
                "F3" -> setCameraMode(CameraMode.FREE_CAMERA)
            }
        }
    }
    
    /**
     * Set camera mode with keyboard shortcut
     */
    suspend fun setCameraMode(mode: CameraMode) {
        cameraMode = mode
        println("DesktopCameraUI: Camera mode changed to $mode")
        displayCameraControls()
    }
    
    private fun displayCameraControls() {
        println("DesktopCameraUI: ┌─── Camera Controls ───┐")
        println("DesktopCameraUI: │ Mode: $cameraMode      │")
        println("DesktopCameraUI: │ WASD: Move            │")
        println("DesktopCameraUI: │ QE: Up/Down           │")
        println("DesktopCameraUI: │ Mouse: Look           │")
        println("DesktopCameraUI: │ F1-F3: Camera modes   │")
        println("DesktopCameraUI: └───────────────────────┘")
    }
}

/**
 * Desktop World Map UI - Resizable map window with advanced features
 */
class DesktopWorldMapUI : UIComponent() {
    
    private var isVisible = false
    private var mapLayer = MapLayer.TERRAIN
    private var showTraffic = false
    private var showFriends = true
    
    override suspend fun applyTheme(theme: UITheme) {
        println("DesktopWorldMapUI: Applied ${theme.name} theme to map window")
    }
    
    override suspend fun show() {
        isVisible = true
        println("DesktopWorldMapUI: Showing world map window")
        displayMapWindow()
    }
    
    override suspend fun hide() {
        isVisible = false
        println("DesktopWorldMapUI: Hiding world map window")
    }
    
    override suspend fun updateLayout(screenSize: ScreenSize) {
        val windowSize = (screenSize.width * 0.4).toInt().coerceAtLeast(400)
        println("DesktopWorldMapUI: Resized map window to ${windowSize}x${windowSize}")
    }
    
    /**
     * Change map layer
     */
    suspend fun setMapLayer(layer: MapLayer) {
        mapLayer = layer
        println("DesktopWorldMapUI: Changed map layer to $layer")
        displayMapWindow()
    }
    
    /**
     * Toggle traffic display
     */
    suspend fun toggleTraffic() {
        showTraffic = !showTraffic
        println("DesktopWorldMapUI: Traffic display ${if (showTraffic) "enabled" else "disabled"}")
    }
    
    /**
     * Search for location
     */
    suspend fun searchLocation(query: String): List<MapLocation> {
        val results = listOf(
            MapLocation("Welcome Area", 100.0f, 100.0f),
            MapLocation("Sandbox", 200.0f, 150.0f),
            MapLocation("Shopping District", 300.0f, 200.0f)
        ).filter { it.name.contains(query, ignoreCase = true) }
        
        println("DesktopWorldMapUI: Location search '$query' found ${results.size} results")
        return results
    }
    
    private fun displayMapWindow() {
        println("DesktopWorldMapUI: ┌─────────────────────────────────┐")
        println("DesktopWorldMapUI: │ World Map - ${mapLayer.name}           │")
        println("DesktopWorldMapUI: ├─────────────────────────────────┤")
        println("DesktopWorldMapUI: │ Layers: [Terrain] [Parcels]     │")
        println("DesktopWorldMapUI: │ Traffic: ${if (showTraffic) "[ON]" else "[OFF]"}  Friends: ${if (showFriends) "[ON]" else "[OFF]"} │")
        println("DesktopWorldMapUI: ├─────────────────────────────────┤")
        println("DesktopWorldMapUI: │          🗺️ MAP VIEW 🗺️          │")
        println("DesktopWorldMapUI: │     📍 You are here (128,128)   │")
        if (showFriends) {
            println("DesktopWorldMapUI: │     👥 Friends: 2 online        │")
        }
        println("DesktopWorldMapUI: └─────────────────────────────────┘")
    }
}

/**
 * Desktop Avatar UI - Comprehensive appearance editor
 */
class DesktopAvatarUI : UIComponent() {
    
    private var isVisible = false
    private val avatarSettings = AvatarSettings()
    
    override suspend fun applyTheme(theme: UITheme) {
        println("DesktopAvatarUI: Applied ${theme.name} theme to avatar editor")
    }
    
    override suspend fun show() {
        isVisible = true
        println("DesktopAvatarUI: Showing avatar appearance editor")
        displayAvatarEditor()
    }
    
    override suspend fun hide() {
        isVisible = false
        println("DesktopAvatarUI: Hiding avatar editor")
    }
    
    override suspend fun updateLayout(screenSize: ScreenSize) {
        val windowWidth = (screenSize.width * 0.35).toInt().coerceAtLeast(350)
        val windowHeight = (screenSize.height * 0.7).toInt().coerceAtLeast(500)
        
        println("DesktopAvatarUI: Resized avatar editor to ${windowWidth}x${windowHeight}")
    }
    
    /**
     * Load avatar preset
     */
    suspend fun loadPreset(presetName: String) {
        println("DesktopAvatarUI: Loading avatar preset: $presetName")
        // Simulate loading preset
        delay(500)
        println("DesktopAvatarUI: Preset '$presetName' loaded successfully")
        displayAvatarEditor()
    }
    
    /**
     * Save current avatar as preset
     */
    suspend fun savePreset(presetName: String) {
        println("DesktopAvatarUI: Saving current avatar as preset: $presetName")
        delay(300)
        println("DesktopAvatarUI: Preset '$presetName' saved successfully")
    }
    
    private fun displayAvatarEditor() {
        println("DesktopAvatarUI: ┌─────────────────────────────────────┐")
        println("DesktopAvatarUI: │ Avatar Appearance Editor            │")
        println("DesktopAvatarUI: ├─────────────────────────────────────┤")
        println("DesktopAvatarUI: │ Body Parts: [Shape] [Skin] [Hair]   │")
        println("DesktopAvatarUI: │ Clothing:   [Shirt] [Pants] [Shoes] │")
        println("DesktopAvatarUI: │ Attachments: [Hat] [Jewelry] [HUD]  │")
        println("DesktopAvatarUI: ├─────────────────────────────────────┤")
        println("DesktopAvatarUI: │        👤 Avatar Preview 👤         │")
        println("DesktopAvatarUI: │         Height: ${avatarSettings.height}m            │")
        println("DesktopAvatarUI: │         Attachments: ${avatarSettings.attachmentCount}           │")
        println("DesktopAvatarUI: └─────────────────────────────────────┘")
    }
}

/**
 * Desktop Layout Manager - Handles desktop windowing system
 */
class DesktopLayoutManager : LayoutManager() {
    
    private val windows = mutableMapOf<String, WindowInfo>()
    
    override suspend fun updateScreenSize(width: Int, height: Int) {
        println("DesktopLayoutManager: Screen resized to ${width}x${height}")
        
        // Adjust window positions if they're off-screen
        windows.values.forEach { window ->
            if (window.x + window.width > width) {
                window.x = (width - window.width).coerceAtLeast(0)
            }
            if (window.y + window.height > height) {
                window.y = (height - window.height).coerceAtLeast(0)
            }
        }
    }
    
    override suspend fun arrangeComponents(components: Map<String, UIComponent>) {
        println("DesktopLayoutManager: Arranging ${components.size} desktop windows")
        
        var xOffset = 50
        var yOffset = 50
        
        components.forEach { (name, component) ->
            val windowInfo = WindowInfo(
                x = xOffset,
                y = yOffset,
                width = 400,
                height = 300,
                resizable = true,
                minimized = false
            )
            
            windows[name] = windowInfo
            println("DesktopLayoutManager: Positioned $name window at ($xOffset, $yOffset)")
            
            xOffset += 50
            yOffset += 50
        }
    }
    
    /**
     * Minimize/restore window
     */
    suspend fun toggleWindowMinimized(componentName: String) {
        val window = windows[componentName]
        if (window != null) {
            window.minimized = !window.minimized
            println("DesktopLayoutManager: ${if (window.minimized) "Minimized" else "Restored"} $componentName window")
        }
    }
}

// Data classes for desktop UI

data class ChatTab(
    val name: String,
    val messages: MutableList<ChatMessage>
)

data class WindowStyle(
    val background: String,
    val border: String,
    val text: String,
    val accent: String
)

sealed class InventoryNode(open val name: String)

data class InventoryFolder(
    override val name: String,
    val children: MutableList<InventoryNode>
) : InventoryNode(name)

data class InventoryItem(
    override val name: String,
    val category: String,
    val description: String
) : InventoryNode(name)

enum class SortField {
    NAME, TYPE, DATE, SIZE
}

enum class MouseButton {
    LEFT, RIGHT, MIDDLE, NONE
}

enum class MapLayer {
    TERRAIN, PARCELS, TRAFFIC, OBJECTS
}

data class MapLocation(
    val name: String,
    val x: Float,
    val y: Float
)

data class AvatarSettings(
    val height: Float = 1.75f,
    val attachmentCount: Int = 3
)

data class WindowInfo(
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,
    val resizable: Boolean,
    var minimized: Boolean
)