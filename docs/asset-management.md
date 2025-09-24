# Asset Management System Documentation

## Overview

The Asset Management system provides comprehensive handling of SecondLife virtual world assets including textures, meshes, sounds, animations, and other content types. This system modernizes the C++ asset pipeline from SecondLife viewers with Kotlin coroutines, type safety, and advanced caching strategies.

## Architecture

### Core Components

#### AssetManager
The central asset management class that handles:
- **Multi-tier Caching**: Memory cache for hot assets, disk cache for persistence
- **Download Queue Management**: Prioritized downloading with bandwidth optimization
- **Asset Processing**: Format conversion, compression, and optimization
- **Statistics Tracking**: Performance monitoring and cache efficiency

#### Asset Types
Based on SecondLife's `llinventorytype.h`:
```kotlin
enum class AssetType(val id: Int, val extension: String) {
    TEXTURE(0, "j2c"),      // JPEG2000 compressed textures
    SOUND(1, "ogg"),        // Ogg Vorbis audio files
    MESH(49, "mesh"),       // 3D mesh data
    ANIMATION(20, "anim"),  // Avatar animations
    SCRIPT(10, "lsl"),      // LSL scripts
    // ... additional types
}
```

#### Asset Data Structure
Type-safe asset representation with metadata:
```kotlin
data class Asset(
    val uuid: UUID,
    val type: AssetType,
    val data: ByteArray,
    val metadata: AssetMetadata,
    val timestamp: Long
)
```

## Key Features

### 1. Multi-Tier Caching System

**Memory Cache**: Instant access for frequently used assets
- LRU eviction policy
- Configurable size limits
- Thread-safe concurrent access

**Disk Cache**: Persistent storage for downloaded assets
- SHA-256 hash verification
- Automatic cleanup based on age and size
- Cross-session persistence

### 2. Prioritized Download Queue

Assets are downloaded based on priority levels:
- **CRITICAL**: User interface elements, current view textures
- **HIGH**: Nearby objects, avatar textures
- **NORMAL**: General world content
- **LOW**: Background preloading, distant objects

### 3. Asset Processing Pipeline

**Texture Processing**:
- JPEG2000 decompression
- Mipmap generation for LOD
- Format conversion (RGBA, DXT compression)
- Size optimization for different quality levels

**Mesh Processing**:
- Binary mesh format parsing
- LOD generation and optimization
- Vertex buffer optimization
- Collision mesh generation

**Audio Processing**:
- Ogg Vorbis decoding
- Resampling for different quality levels
- 3D audio metadata extraction
- Format conversion for platform compatibility

**Animation Processing**:
- Keyframe interpolation
- Bone mapping and validation
- Animation blending preparation
- Compression for network efficiency

### 4. Asset Permissions System

Imported from SecondLife's permission model:
```kotlin
data class AssetPermissions(
    val baseMask: Int,      // Base permissions
    val ownerMask: Int,     // Owner permissions
    val groupMask: Int,     // Group permissions
    val everyoneMask: Int,  // Public permissions
    val nextOwnerMask: Int  // Transfer permissions
)
```

Permission flags:
- `PERM_TRANSFER`: Can be transferred to others
- `PERM_MODIFY`: Can be modified
- `PERM_COPY`: Can be copied
- `PERM_MOVE`: Can be moved in inventory

## Usage Examples

### Basic Asset Loading
```kotlin
val assetManager = AssetManager(eventSystem)
await assetManager.initialize()

// Load a texture
val texture = assetManager.getAsset(
    uuid = textureUUID,
    type = AssetType.TEXTURE,
    priority = Priority.HIGH
)
```

### Batch Asset Loading
```kotlin
val requests = listOf(
    textureUUID1 to AssetType.TEXTURE,
    meshUUID1 to AssetType.MESH,
    soundUUID1 to AssetType.SOUND
)

val assets = assetManager.getAssets(requests)
```

### Asset Preloading
```kotlin
// Preload assets for better user experience
val nearbyTextures = listOf(uuid1, uuid2, uuid3)
assetManager.preloadAssets(nearbyTextures, AssetType.TEXTURE)
```

### Asset Status Monitoring
```kotlin
when (assetManager.getAssetStatus(uuid)) {
    AssetStatus.READY -> { /* Asset is loaded and ready */ }
    AssetStatus.DOWNLOADING -> { /* Asset is being downloaded */ }
    AssetStatus.CACHED -> { /* Asset is in disk cache */ }
    AssetStatus.NOT_FOUND -> { /* Asset needs to be downloaded */ }
}
```

## Performance Optimization

### 1. Intelligent Caching
- **Spatial Locality**: Prioritize assets near the user
- **Temporal Locality**: Keep recently used assets in memory
- **Predictive Loading**: Preload assets based on movement patterns

### 2. Network Optimization
- **Connection Pooling**: Reuse HTTP connections for asset downloads
- **Compression**: Use HTTP compression for asset transfers
- **CDN Integration**: Support for content delivery networks

### 3. Memory Management
- **Streaming**: Load large assets progressively
- **Compression**: Use compressed formats in memory when possible
- **Garbage Collection**: Automatic cleanup of unused assets

## Asset Security

### 1. Content Validation
- **Hash Verification**: SHA-256 hash checking for integrity
- **Format Validation**: Ensure assets match expected formats
- **Size Limits**: Prevent memory exhaustion attacks

### 2. Permission Enforcement
- **Access Control**: Respect asset permissions
- **Transfer Restrictions**: Honor no-transfer flags
- **Modification Rights**: Check modify permissions before processing

### 3. Sandboxing
- **Isolated Processing**: Process assets in isolated contexts
- **Resource Limits**: Enforce CPU and memory limits during processing
- **Error Handling**: Graceful handling of malformed assets

## Integration Points

### Protocol System Integration
```kotlin
// Assets are automatically requested when protocol receives object data
eventSystem.events.collect { event ->
    when (event) {
        is ViewerEvent.ObjectReceived -> {
            // Load textures and meshes for the object
            loadObjectAssets(event.objectData)
        }
    }
}
```

### Graphics System Integration
```kotlin
// Graphics system requests assets for rendering
val texture = assetManager.getAsset(textureUUID, AssetType.TEXTURE)
if (texture != null) {
    renderSystem.loadTexture(texture)
}
```

### Audio System Integration
```kotlin
// Audio system loads sound assets
val sound = assetManager.getAsset(soundUUID, AssetType.SOUND)
if (sound != null) {
    audioSystem.playSound(sound, position)
}
```

## Monitoring and Debugging

### Asset Statistics
```kotlin
val stats = assetManager.getStats()
println("Cache Hit Rate: ${stats.hitRate}")
println("Download Success Rate: ${stats.successRate}")
println("Memory Usage: ${stats.bytesServed}")
```

### Performance Profiling
- **Download Times**: Track asset download performance
- **Processing Times**: Monitor asset processing overhead
- **Cache Efficiency**: Analyze cache hit/miss patterns
- **Memory Usage**: Track memory consumption patterns

### Debug Tools
- **Asset Inspector**: View asset metadata and status
- **Cache Browser**: Explore cached assets
- **Download Monitor**: Real-time download tracking
- **Performance Graphs**: Visualize system performance

## Configuration

### Cache Settings
```kotlin
val assetManager = AssetManager(
    eventSystem = eventSystem,
    cacheDirectory = File("./cache"),
    maxCacheSize = 1024L * 1024L * 1024L // 1GB
)
```

### Performance Tuning
- **Memory Cache Size**: Balance memory usage vs. performance
- **Disk Cache Size**: Configure based on available storage
- **Download Workers**: Adjust concurrent download limits
- **Processing Threads**: Configure based on CPU cores

## Future Enhancements

### 1. Advanced Features
- **Progressive Loading**: Stream large assets progressively
- **Delta Updates**: Download only changed portions of assets
- **Peer-to-Peer**: Asset sharing between nearby users
- **Blockchain Integration**: Decentralized asset verification

### 2. Platform Optimization
- **Mobile Optimization**: Reduced memory usage for mobile devices
- **GPU Acceleration**: Hardware-accelerated asset processing
- **Cloud Integration**: Cloud-based asset processing and storage
- **Edge Caching**: Geographic distribution of asset servers

### 3. Quality Improvements
- **Machine Learning**: Predictive asset loading based on user behavior
- **Adaptive Quality**: Dynamic quality adjustment based on bandwidth
- **Smart Compression**: Context-aware compression algorithms
- **Real-time Optimization**: Live asset optimization during gameplay

This asset management system provides a solid foundation for handling all types of virtual world content with modern performance, security, and maintainability standards.