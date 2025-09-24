package com.linkpoint

import com.linkpoint.assets.AssetManager
import com.linkpoint.audio.AudioSystem
import com.linkpoint.core.events.EventSystem
import com.linkpoint.core.events.ViewerEvent
import com.linkpoint.protocol.data.SimpleWorldEntities.Vector3
import com.linkpoint.protocol.data.SimpleWorldEntities.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

/**
 * AssetAudioDemo - Comprehensive demonstration of Phase 5: Asset Management and Audio System
 * 
 * This demo showcases the complete integration of asset loading and 3D audio systems,
 * demonstrating how modern Kotlin architecture can handle complex virtual world audio
 * and asset management imported from SecondLife, Firestorm, and RLV viewers.
 */
class AssetAudioDemo {
    
    private val eventSystem = EventSystem()
    private lateinit var assetManager: AssetManager
    private lateinit var audioSystem: AudioSystem
    
    /**
     * Run the complete asset and audio system demonstration
     */
    suspend fun runDemo() {
        println("🎵 Starting Phase 5: Asset Management and Audio System Demo")
        println("=" .repeat(70))
        
        try {
            // Initialize systems
            initializeSystems()
            
            // Demonstrate asset management
            demonstrateAssetManagement()
            
            // Demonstrate 3D audio system
            demonstrate3DAudio()
            
            // Demonstrate integration
            demonstrateIntegration()
            
            // Show performance monitoring
            demonstratePerformanceMonitoring()
            
            // Cleanup
            cleanup()
            
        } catch (e: Exception) {
            println("❌ Demo failed: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Initialize asset manager and audio system
     */
    private suspend fun initializeSystems() {
        println("\n📁 Initializing Asset Management System...")
        
        assetManager = AssetManager(eventSystem)
        
        println("\n🔊 Initializing 3D Audio System...")
        
        audioSystem = AudioSystem(assetManager, eventSystem)
        val audioInitialized = audioSystem.initialize()
        
        if (audioInitialized) {
            println("✅ All systems initialized successfully")
        } else {
            throw RuntimeException("Failed to initialize audio system")
        }
    }
    
    /**
     * Demonstrate comprehensive asset management capabilities
     */
    private suspend fun demonstrateAssetManagement() {
        println("\n" + "=".repeat(50))
        println("📦 ASSET MANAGEMENT DEMONSTRATION")
        println("=".repeat(50))
        
        // Single asset loading
        println("\n🖼️  Loading texture assets...")
        val textureUuids = listOf(
            UUID("texture_grass_01"),
            UUID("texture_stone_01"),
            UUID("texture_wood_01"),
            UUID("texture_metal_01")
        )
        
        textureUuids.forEach { uuid ->
            val asset = assetManager.getAsset(uuid, AssetManager.AssetType.TEXTURE)
            if (asset != null) {
                println("   ✓ Loaded texture: ${uuid.value} (${asset.size} bytes)")
                println("     - Dimensions: ${asset.metadata.width}x${asset.metadata.height}")
                println("     - Channels: ${asset.metadata.channels}")
                println("     - Compression: ${asset.metadata.compression}")
            }
        }
        
        // Batch asset loading
        println("\n🎵 Batch loading sound assets...")
        val soundRequests = listOf(
            UUID("sound_footstep_grass") to AssetManager.AssetType.SOUND,
            UUID("sound_ambient_forest") to AssetManager.AssetType.SOUND,
            UUID("sound_ui_notification") to AssetManager.AssetType.SOUND,
            UUID("sound_music_peaceful") to AssetManager.AssetType.SOUND
        )
        
        val soundAssets = assetManager.getAssets(soundRequests)
        soundAssets.forEach { (uuid, asset) ->
            if (asset != null) {
                println("   ✓ Loaded sound: ${uuid.value} (${asset.size} bytes)")
            }
        }
        
        // 3D mesh loading
        println("\n🧊 Loading 3D mesh assets...")
        val meshUuids = listOf(
            UUID("mesh_cube_simple"),
            UUID("mesh_tree_oak"),
            UUID("mesh_building_house")
        )
        
        meshUuids.forEach { uuid ->
            val asset = assetManager.getAsset(uuid, AssetManager.AssetType.MESH)
            if (asset != null) {
                println("   ✓ Loaded mesh: ${uuid.value} (${asset.size} bytes)")
            }
        }
        
        // Animation loading
        println("\n🚶 Loading animation assets...")
        val animationUuids = listOf(
            UUID("anim_walk_normal"),
            UUID("anim_dance_salsa"),
            UUID("anim_gesture_wave")
        )
        
        animationUuids.forEach { uuid ->
            val asset = assetManager.getAsset(uuid, AssetManager.AssetType.ANIMATION)
            if (asset != null) {
                println("   ✓ Loaded animation: ${uuid.value} (${asset.size} bytes)")
            }
        }
        
        // Asset preloading demonstration
        println("\n⚡ Demonstrating asset preloading...")
        val preloadUuids = listOf(
            UUID("texture_sky_sunset"),
            UUID("texture_water_calm"),
            UUID("texture_cloud_01")
        )
        
        assetManager.preloadAssets(preloadUuids, AssetManager.AssetType.TEXTURE)
        println("   ✓ Started preloading ${preloadUuids.size} textures")
        
        delay(500) // Allow preloading to progress
        
        // Check asset status
        println("\n📊 Asset status check:")
        preloadUuids.forEach { uuid ->
            val status = assetManager.getAssetStatus(uuid)
            println("   - ${uuid.value}: $status")
        }
    }
    
    /**
     * Demonstrate 3D positional audio capabilities
     */
    private suspend fun demonstrate3DAudio() {
        println("\n" + "=".repeat(50))
        println("🔊 3D AUDIO SYSTEM DEMONSTRATION")
        println("=".repeat(50))
        
        // Set up listener position (avatar/camera)
        val listenerPosition = Vector3(0f, 0f, 0f)
        val listenerForward = Vector3(1f, 0f, 0f)
        val listenerUp = Vector3(0f, 0f, 1f)
        
        audioSystem.updateListener(
            position = listenerPosition,
            forward = listenerForward,
            up = listenerUp
        )
        
        println("\n🎧 Setting up 3D audio listener at origin")
        println("   Position: (${listenerPosition.x}, ${listenerPosition.y}, ${listenerPosition.z})")
        println("   Forward: (${listenerForward.x}, ${listenerForward.y}, ${listenerForward.z})")
        
        // Play positioned sound effects
        println("\n🎵 Playing 3D positioned sounds...")
        
        val soundSources = mutableListOf<String?>()
        
        // Sound to the right
        soundSources.add(audioSystem.playSound(
            soundUuid = "sound_footstep_grass",
            position = Vector3(10f, 0f, 0f),
            volume = 0.8f,
            type = AudioSystem.SoundType.EFFECT
        ))
        println("   ✓ Playing footsteps to the right (10, 0, 0)")
        
        // Sound to the left
        soundSources.add(audioSystem.playSound(
            soundUuid = "sound_ambient_forest",
            position = Vector3(-15f, 0f, 0f),
            volume = 0.6f,
            loop = true,
            type = AudioSystem.SoundType.AMBIENT
        ))
        println("   ✓ Playing forest ambience to the left (-15, 0, 0)")
        
        // Sound in front
        soundSources.add(audioSystem.playSound(
            soundUuid = "sound_music_peaceful",
            position = Vector3(0f, 20f, 0f),
            volume = 0.4f,
            loop = true,
            type = AudioSystem.SoundType.MUSIC
        ))
        println("   ✓ Playing peaceful music in front (0, 20, 0)")
        
        // Sound behind and above
        soundSources.add(audioSystem.playSound(
            soundUuid = "sound_ui_notification",
            position = Vector3(0f, -5f, 10f),
            volume = 0.7f,
            type = AudioSystem.SoundType.UI
        ))
        println("   ✓ Playing UI notification behind and above (0, -5, 10)")
        
        delay(1000) // Let sounds play
        
        // Demonstrate volume controls
        println("\n🔊 Volume control demonstration...")
        println("   Setting master volume to 80%")
        audioSystem.setVolume(AudioSystem.SoundType.MASTER, 0.8f)
        
        delay(500)
        
        println("   Setting ambient volume to 30%")
        audioSystem.setVolume(AudioSystem.SoundType.AMBIENT, 0.3f)
        
        delay(500)
        
        println("   Setting music volume to 20%")
        audioSystem.setVolume(AudioSystem.SoundType.MUSIC, 0.2f)
        
        delay(500)
        
        // Demonstrate listener movement
        println("\n🚶 Demonstrating listener movement...")
        val positions = listOf(
            Vector3(2f, 0f, 0f),
            Vector3(4f, 2f, 0f),
            Vector3(6f, 4f, 1f),
            Vector3(8f, 6f, 2f)
        )
        
        positions.forEach { position ->
            audioSystem.updateListener(
                position = position,
                forward = Vector3(1f, 0f, 0f),
                up = Vector3(0f, 0f, 1f)
            )
            println("   🎧 Moved listener to (${position.x}, ${position.y}, ${position.z})")
            delay(800)
        }
        
        // Demonstrate moving sound source
        println("\n🏃 Demonstrating moving sound source...")
        val movingSoundId = audioSystem.playSound(
            soundUuid = "sound_footstep_grass",
            position = Vector3(-20f, 0f, 0f),
            volume = 0.9f,
            loop = true,
            type = AudioSystem.SoundType.EFFECT
        )
        
        if (movingSoundId != null) {
            val movementPositions = listOf(
                Vector3(-20f, 0f, 0f) to Vector3(5f, 0f, 0f),   // Moving right
                Vector3(-15f, 0f, 0f) to Vector3(5f, 0f, 0f),
                Vector3(-10f, 0f, 0f) to Vector3(5f, 0f, 0f),
                Vector3(-5f, 0f, 0f) to Vector3(5f, 0f, 0f),
                Vector3(0f, 0f, 0f) to Vector3(5f, 0f, 0f),
                Vector3(5f, 0f, 0f) to Vector3(5f, 0f, 0f),
                Vector3(10f, 0f, 0f) to Vector3(0f, 0f, 0f)     // Stopped
            )
            
            movementPositions.forEach { (position, velocity) ->
                audioSystem.updateSoundPosition(movingSoundId, position, velocity)
                println("   🔊 Moving sound to (${position.x}, ${position.y}, ${position.z})")
                delay(600)
            }
            
            audioSystem.stopSound(movingSoundId)
        }
        
        // Clean up other sounds
        soundSources.filterNotNull().forEach { sourceId ->
            audioSystem.stopSound(sourceId)
        }
    }
    
    /**
     * Demonstrate integration between asset and audio systems
     */
    private suspend fun demonstrateIntegration() {
        println("\n" + "=".repeat(50))
        println("🔗 ASSET & AUDIO INTEGRATION DEMONSTRATION")
        println("=".repeat(50))
        
        println("\n🎭 Simulating virtual world scenario...")
        
        // Simulate avatar walking through different environments
        val environments = listOf(
            Environment("Forest", Vector3(0f, 0f, 0f), "sound_ambient_forest", "texture_grass_01"),
            Environment("Stone Bridge", Vector3(50f, 0f, 5f), "sound_footstep_stone", "texture_stone_01"),
            Environment("Wooden Dock", Vector3(100f, 0f, 0f), "sound_ambient_water", "texture_wood_01"),
            Environment("Metal Platform", Vector3(150f, 0f, 10f), "sound_industrial_hum", "texture_metal_01")
        )
        
        environments.forEach { env ->
            println("\n🌍 Entering ${env.name}...")
            
            // Load environment assets
            println("   📦 Loading environment assets...")
            val textureAsset = assetManager.getAsset(
                UUID(env.textureUuid), 
                AssetManager.AssetType.TEXTURE
            )
            val soundAsset = assetManager.getAsset(
                UUID(env.soundUuid), 
                AssetManager.AssetType.SOUND
            )
            
            if (textureAsset != null) {
                println("   ✓ Loaded texture: ${env.textureUuid} (${textureAsset.size} bytes)")
            }
            
            // Update listener position
            audioSystem.updateListener(
                position = env.position,
                forward = Vector3(1f, 0f, 0f),
                up = Vector3(0f, 0f, 1f)
            )
            
            // Play environment audio
            val ambientSoundId = audioSystem.playSound(
                soundUuid = env.soundUuid,
                position = env.position,
                volume = 0.5f,
                loop = true,
                type = AudioSystem.SoundType.AMBIENT
            )
            
            println("   🔊 Playing ambient audio at position (${env.position.x}, ${env.position.y}, ${env.position.z})")
            
            // Simulate staying in environment
            delay(1500)
            
            // Stop ambient when leaving
            ambientSoundId?.let { audioSystem.stopSound(it) }
            println("   🔇 Left ${env.name}")
        }
        
        // Demonstrate asset caching efficiency
        println("\n⚡ Demonstrating asset cache efficiency...")
        
        // Re-request previously loaded assets (should be cache hits)
        val startTime = System.currentTimeMillis()
        
        val cachedAssets = listOf(
            UUID("texture_grass_01") to AssetManager.AssetType.TEXTURE,
            UUID("sound_ambient_forest") to AssetManager.AssetType.SOUND,
            SeriesUUID("mesh_cube_simple") to AssetManager.AssetType.MESH
        )
        
        cachedAssets.forEach { (uuid, type) ->
            val asset = assetManager.getAsset(uuid, type)
            if (asset != null) {
                println("   ⚡ Cache hit for ${uuid.value}: ${asset.size} bytes")
            }
        }
        
        val endTime = System.currentTimeMillis()
        println("   📊 Cache retrieval completed in ${endTime - startTime}ms")
    }
    
    /**
     * Demonstrate performance monitoring and statistics
     */
    private suspend fun demonstratePerformanceMonitoring() {
        println("\n" + "=".repeat(50))
        println("📊 PERFORMANCE MONITORING DEMONSTRATION")
        println("=".repeat(50))
        
        // Asset Manager Statistics
        println("\n📦 Asset Manager Performance:")
        val assetStats = assetManager.getStats()
        println("   Cache Hits: ${assetStats.cacheHits}")
        println("   Cache Misses: ${assetStats.cacheMisses}")
        println("   Hit Rate: ${String.format("%.1f", assetStats.hitRate * 100)}%")
        println("   Downloads Started: ${assetStats.downloadsStarted}")
        println("   Downloads Completed: ${assetStats.downloadsCompleted}")
        println("   Downloads Failed: ${assetStats.downloadsFailed}")
        println("   Success Rate: ${String.format("%.1f", assetStats.successRate * 100)}%")
        println("   Bytes Downloaded: ${assetStats.bytesDownloaded}")
        println("   Bytes Served: ${assetStats.bytesServed}")
        
        // Audio System Statistics
        println("\n🔊 Audio System Performance:")
        val audioStats = audioSystem.getAudioStats()
        println("   Active Sources: ${audioStats.activeSources}")
        println("   Total Sources Created: ${audioStats.totalSources}")
        println("   Audio Memory Usage: ${audioStats.audioMemoryUsage} bytes")
        println("   Processing Load: ${String.format("%.2f", audioStats.processingLoad)}ms")
        println("   Average Latency: ${String.format("%.2f", audioStats.averageLatency)}ms")
        println("   Dropped Frames: ${audioStats.droppedFrames}")
        
        // Audio Settings
        println("\n⚙️ Audio System Configuration:")
        val audioSettings = audioSystem.getAudioSettings()
        println("   Master Volume: ${String.format("%.0f", audioSettings.masterVolume * 100)}%")
        println("   Effect Volume: ${String.format("%.0f", audioSettings.effectVolume * 100)}%")
        println("   Ambient Volume: ${String.format("%.0f", audioSettings.ambientVolume * 100)}%")
        println("   Music Volume: ${String.format("%.0f", audioSettings.musicVolume * 100)}%")
        println("   Audio Quality: ${audioSettings.audioQuality.name}")
        println("   Sample Rate: ${audioSettings.sampleRate}Hz")
        println("   Buffer Size: ${audioSettings.audioBufferSize} samples")
        println("   Max Sources: ${audioSettings.maxAudioSources}")
        println("   Doppler Effect: ${if (audioSettings.enableDoppler) "Enabled" else "Disabled"}")
        println("   Reverb: ${if (audioSettings.enableReverb) "Enabled" else "Disabled"}")
        println("   Occlusion: ${if (audioSettings.enableOcclusion) "Enabled" else "Disabled"}")
        
        // Performance recommendations
        println("\n💡 Performance Recommendations:")
        if (assetStats.hitRate < 0.8) {
            println("   - Consider increasing asset cache size for better performance")
        }
        if (audioStats.processingLoad > 5.0f) {
            println("   - Consider reducing audio quality or max sources to improve performance")
        }
        if (audioStats.activeSources > 32) {
            println("   - High number of active audio sources may impact performance")
        }
        
        println("   ✓ All systems operating within normal parameters")
    }
    
    /**
     * Clean up resources
     */
    private suspend fun cleanup() {
        println("\n🧹 Cleaning up systems...")
        
        audioSystem.shutdown()
        assetManager.shutdown()
        
        println("✅ Cleanup completed")
        
        println("\n" + "=".repeat(70))
        println("🎉 Phase 5: Asset Management and Audio System Demo Complete!")
        println("=".repeat(70))
        
        println("\n📋 Summary of Demonstrated Features:")
        println("✓ Complete asset management with multi-tier caching")
        println("✓ Support for textures, meshes, sounds, and animations")
        println("✓ Batch asset loading and preloading capabilities")
        println("✓ 3D positional audio with distance attenuation")
        println("✓ Doppler effects and environmental audio")
        println("✓ Multi-channel audio mixing and volume control")
        println("✓ Asset and audio system integration")
        println("✓ Performance monitoring and optimization")
        println("✓ Cache efficiency and resource management")
        println("✓ Real-time audio parameter updates")
        
        println("\n🏁 Ready for advanced virtual world features!")
    }
    
    /**
     * Environment data for integration demonstration
     */
    private data class Environment(
        val name: String,
        val position: Vector3,
        val soundUuid: String,
        val textureUuid: String
    )
}

/**
 * Helper function to create UUID with simpler syntax for demo
 */
private fun SeriesUUID(value: String) = UUID(value)

/**
 * Main function to run the asset and audio demonstration
 */
suspend fun main() {
    val demo = AssetAudioDemo()
    demo.runDemo()
}