# Component Mapping: C++ to Kotlin

This document provides detailed mapping of specific C++ components from the source viewers to their planned Kotlin equivalents.

## SecondLife Viewer Components

### Core Application Framework

| C++ Component | File | Kotlin Equivalent | Purpose |
|---------------|------|-------------------|---------|
| `LLAppViewer` | `llappviewer.cpp` | `ViewerCore` | Main application lifecycle |
| `LLEventPump` | `lleventpump.cpp` | `EventSystem` | Event handling system |
| `LLSingleton<T>` | `llsingleton.h` | `object` classes | Singleton management |
| `LLInitClass` | `llinitclass.h` | `InitializationManager` | Startup initialization |

### Network and Protocol

| C++ Component | File | Kotlin Equivalent | Purpose |
|---------------|------|-------------------|---------|
| `LLMessageSystem` | `llmessagesystem.cpp` | `MessageSystem` | UDP message handling |
| `LLViewerMessage` | `llviewermessage.cpp` | `MessageProcessor` | Message processing logic |
| `LLLoginHandler` | `llloginhandler.cpp` | `LoginHandler` | XMLRPC authentication |
| `LLViewerRegion` | `llviewerregion.cpp` | `Region` | Simulator region management |
| `LLCircuitData` | `llcircuit.cpp` | `CircuitManager` | Network circuit management |

### Rendering System

| C++ Component | File | Kotlin Equivalent | Purpose |
|---------------|------|-------------------|---------|
| `LLPipeline` | `llpipeline.cpp` | `RenderPipeline` | Main rendering pipeline |
| `LLDrawPoolManager` | `lldrawpoolmanager.cpp` | `RenderEngine` | Render queue management |
| `LLVOAvatar` | `llvoavatar.cpp` | `Avatar` | Avatar rendering |
| `LLViewerObject` | `llviewerobject.cpp` | `WorldObject` | 3D object representation |
| `LLFace` | `llface.cpp` | `RenderFace` | Renderable surface |

### User Interface

| C++ Component | File | Kotlin Equivalent | Purpose |
|---------------|------|-------------------|---------|
| `LLFloater` | `llfloater.cpp` | `Window` | Base window class |
| `LLChatBar` | `llchatbar.cpp` | `ChatInterface` | Chat input/output |
| `LLInventoryModel` | `llinventorymodel.cpp` | `InventoryManager` | Inventory management |
| `LLPanelLogin` | `llpanellogin.cpp` | `LoginScreen` | Login interface |

## Firestorm Viewer Enhancements

### Performance Optimizations

| C++ Component | Directory/File | Kotlin Equivalent | Enhancement |
|---------------|----------------|-------------------|-------------|
| Advanced LOD | `pipeline/` | `LevelOfDetailManager` | Improved object detail scaling |
| Texture Cache | `llviewertexture*` | `TextureCache` | Better texture memory management |
| Render Stats | `llstatviewer.cpp` | `PerformanceMonitor` | Real-time performance tracking |

### UI Improvements

| C++ Component | File | Kotlin Equivalent | Enhancement |
|---------------|------|-------------------|-------------|
| Phoenix Menu | `fsfloaterphoenix.cpp` | `AdvancedMenu` | Extended functionality menu |
| Area Search | `fsfloaterareasearch.cpp` | `AdvancedSearch` | Enhanced object search |
| Contact Sets | `fscontactsfloater.cpp` | `ContactManager` | Advanced friend management |

## Restrained Love Viewer (RLV) Components

### Core RLV System

| C++ Component | File | Kotlin Equivalent | Purpose |
|---------------|------|-------------------|---------|
| `RlvHandler` | `rlvhandler.cpp` | `RLVProcessor` | Main RLV command processing |
| `RlvBehaviourManager` | `rlvbehaviourmanager.cpp` | `RLVBehaviorManager` | Behavior restriction management |
| `RlvAttachmentManager` | `rlvattachmentmanager.cpp` | `RLVAttachmentManager` | Attachment control system |

### Extended Protocol

| C++ Component | File | Kotlin Equivalent | Purpose |
|---------------|------|-------------------|---------|
| RLV Commands | `rlvcommands.cpp` | `RLVCommands` | Command parsing and execution |
| RLV Folders | `rlvfolders.cpp` | `RLVFolderManager` | Special folder handling |
| RLV UI Controls | Various | `RLVUIController` | UI restriction management |

## Implementation Approach

### 1. Direct Translation Components
Components that map directly with minimal changes:

```kotlin
// Example: LLEventPump -> EventSystem
object EventSystem {
    private val _events = MutableSharedFlow<ViewerEvent>()
    val events: SharedFlow<ViewerEvent> = _events.asSharedFlow()
    
    suspend fun emit(event: ViewerEvent) {
        _events.emit(event)
    }
}
```

### 2. Modernized Components  
Components that benefit from Kotlin language features:

```kotlin
// Example: LLSingleton -> Kotlin object
object TextureManager {
    private val textureCache = mutableMapOf<String, Texture>()
    
    suspend fun getTexture(uuid: String): Texture? {
        return textureCache[uuid] ?: loadTexture(uuid)
    }
}
```

### 3. Architectural Improvements
Components that are redesigned for better architecture:

```kotlin
// Example: Message system with coroutines
class MessageSystem(private val scope: CoroutineScope) {
    
    suspend fun processMessages() {
        networkChannel.consumeEach { rawMessage ->
            launch { processMessage(rawMessage) }
        }
    }
}
```

## Integration Points

### Core System Integration
```kotlin
class ViewerCore {
    private val messageSystem = MessageSystem(coroutineScope)
    private val renderEngine = RenderEngine(coroutineScope)
    private val eventSystem = EventSystem
    
    suspend fun initialize() {
        messageSystem.initialize()
        renderEngine.initialize()
        // Connect systems through EventSystem
    }
}
```

### Cross-Module Communication
All modules communicate through the centralized EventSystem:

```kotlin
// Protocol module emits events
EventSystem.emit(ViewerEvent.ObjectAdded(objectId, properties))

// Graphics module listens for events
EventSystem.events
    .filterIsInstance<ViewerEvent.ObjectAdded>()
    .onEach { event -> addObjectToRenderQueue(event.objectId) }
    .launchIn(scope)
```

## Testing Strategy

### Unit Testing
Each Kotlin component will have comprehensive unit tests:

```kotlin
class MessageSystemTest {
    @Test
    fun `should process login response correctly`() {
        // Test individual message processing
    }
}
```

### Integration Testing
Test interactions between imported components:

```kotlin
class ViewerIntegrationTest {
    @Test
    fun `should handle complete login flow`() {
        // Test full login sequence across multiple modules
    }
}
```

### Compatibility Testing
Verify protocol compatibility with existing SecondLife infrastructure:

```kotlin
class ProtocolCompatibilityTest {
    @Test
    fun `should connect to SecondLife main grid`() {
        // Test actual connection to live grid
    }
}
```

This mapping provides a clear roadmap for importing and modernizing the C++ codebase into a well-structured Kotlin application.