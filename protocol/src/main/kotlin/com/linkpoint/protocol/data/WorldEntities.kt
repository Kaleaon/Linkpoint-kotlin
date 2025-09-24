package com.linkpoint.protocol.data

import com.linkpoint.core.events.Vector3
import com.linkpoint.core.events.Quaternion
import java.util.UUID

/**
 * Data structures representing virtual world entities in SecondLife/OpenSim compatible format.
 * 
 * These classes model the core entities that exist in virtual worlds, imported and modernized
 * from the SecondLife viewer's object and avatar representation:
 * - Original C++: llviewerobject.cpp, llvoavatar.cpp, llvoinventorylistener.cpp
 * - Firestorm enhancements: Advanced object properties and avatar attachments
 * - Modern Kotlin: Data classes, null safety, immutable by default, type safety
 * 
 * The virtual world contains several types of entities:
 * - Avatars: User-controlled characters with animation and attachments
 * - Objects: Scriptable 3D objects (prims) that can be interactive
 * - Terrain: The landscape and ground of the virtual world
 * - Particles: Visual effects like fire, water, smoke
 * - Audio: Spatial sound sources and ambient audio
 */

/**
 * Base class for all entities that exist in the virtual world
 * Provides common properties shared by all world objects
 */
sealed class WorldEntity {
    abstract val id: UUID
    abstract val name: String
    abstract val position: Vector3
    abstract val rotation: Quaternion
    abstract val scale: Vector3
    abstract val lastUpdate: Long
}

/**
 * Represents an avatar (user character) in the virtual world
 * 
 * Avatars are the representation of users in the virtual world. They can move,
 * gesture, wear attachments, and interact with objects. This data structure
 * captures the essential avatar properties needed for rendering and simulation.
 * 
 * Based on SecondLife viewer's LLVOAvatar class structure
 */
data class Avatar(
    override val id: UUID,
    override val name: String,
    override val position: Vector3,
    override val rotation: Quaternion,
    override val scale: Vector3 = Vector3(1.0f, 1.0f, 1.0f), // Avatars typically don't scale
    override val lastUpdate: Long = System.currentTimeMillis(),
    
    // Avatar-specific properties
    val displayName: String, // Display name (can be different from username)
    val username: String,    // Legacy first.last username format
    val appearanceHash: String, // Hash of avatar appearance for caching
    val animationState: AnimationState,
    val attachments: List<Attachment>,
    val isTyping: Boolean = false,
    val isSitting: Boolean = false,
    val health: Float = 100.0f, // For combat/damage systems
    val energy: Float = 100.0f  // For stamina/movement systems
) : WorldEntity()

/**
 * Represents avatar animation state
 * Animations control how the avatar moves and poses
 */
data class AnimationState(
    val currentAnimation: String, // e.g., "walk", "run", "sit", "dance"
    val animationSpeed: Float = 1.0f,
    val looping: Boolean = true,
    val startTime: Long = System.currentTimeMillis(),
    val blendWeight: Float = 1.0f // For animation blending
)

/**
 * Represents an object attached to an avatar
 * Attachments can be clothing, jewelry, weapons, tools, etc.
 */
data class Attachment(
    val id: UUID,
    val name: String,
    val attachmentPoint: AttachmentPoint,
    val position: Vector3, // Relative to attachment point
    val rotation: Quaternion,
    val scale: Vector3,
    val textureId: UUID? = null,
    val isVisible: Boolean = true
)

/**
 * Standard avatar attachment points defined by SecondLife protocol
 * These correspond to the standard attachment points available in viewers
 */
enum class AttachmentPoint(val id: Int, val displayName: String) {
    // Head attachments
    SKULL(1, "Skull"),
    HEAD_TOP(2, "Top of Head"),
    HEAD_STRETCH(3, "Head Stretch"),
    JAW(4, "Jaw"),
    
    // Torso attachments  
    CHEST(5, "Chest"),
    LEFT_SHOULDER(6, "Left Shoulder"),
    RIGHT_SHOULDER(7, "Right Shoulder"),
    LEFT_HAND(8, "Left Hand"),
    RIGHT_HAND(9, "Right Hand"),
    LEFT_FOOT(10, "Left Foot"),
    RIGHT_FOOT(11, "Right Foot"),
    SPINE(12, "Spine"),
    PELVIS(13, "Pelvis"),
    
    // Additional points
    NOSE(14, "Nose"),
    RIGHT_EAR(15, "Right Ear"),
    LEFT_EAR(16, "Left Ear"),
    LEFT_EYE(17, "Left Eye"),
    RIGHT_EYE(18, "Right Eye"),
    TAIL_BASE(19, "Tail Base"),
    TAIL_TIP(20, "Tail Tip"),
    LEFT_WING(21, "Left Wing"),
    RIGHT_WING(22, "Right Wing"),
    
    // HUD attachments (visible only to wearer)
    HUD_CENTER_2(31, "HUD Center 2"),
    HUD_TOP_RIGHT(32, "HUD Top Right"),
    HUD_TOP_CENTER(33, "HUD Top Center"),
    HUD_TOP_LEFT(34, "HUD Top Left"),
    HUD_CENTER_1(35, "HUD Center 1"),
    HUD_BOTTOM_LEFT(36, "HUD Bottom Left"),
    HUD_BOTTOM(37, "HUD Bottom"),
    HUD_BOTTOM_RIGHT(38, "HUD Bottom Right");
    
    companion object {
        fun fromId(id: Int): AttachmentPoint? = values().find { it.id == id }
    }
}

/**
 * Represents a virtual object (prim) in the world
 * 
 * Objects are the building blocks of virtual world content. They can be scripted,
 * textured, linked together, and made interactive. This represents the core
 * properties needed to render and simulate objects.
 * 
 * Based on SecondLife viewer's LLViewerObject class structure
 */
data class VirtualObject(
    override val id: UUID,
    override val name: String,
    override val position: Vector3,
    override val rotation: Quaternion,
    override val scale: Vector3,
    override val lastUpdate: Long = System.currentTimeMillis(),
    
    // Object-specific properties
    val description: String,
    val creatorId: UUID,
    val ownerId: UUID,
    val groupId: UUID? = null,
    val objectType: ObjectType,
    val material: ObjectMaterial,
    val textureIds: List<UUID>, // Textures for each face
    val isPhysical: Boolean = false,
    val isTemporary: Boolean = false,
    val isPhantom: Boolean = false, // Can other objects pass through?
    val isScripted: Boolean = false,
    val parentId: UUID? = null, // For linked objects
    val children: List<UUID> = emptyList(), // Child objects if this is root
    val touchHandler: String? = null, // Script function for touch events
    val velocity: Vector3 = Vector3(0f, 0f, 0f),
    val angularVelocity: Vector3 = Vector3(0f, 0f, 0f)
) : WorldEntity()

/**
 * Types of virtual objects
 * Different object types have different rendering and physics behaviors
 */
enum class ObjectType {
    PRIMITIVE,    // Basic geometric shapes (cube, sphere, cylinder, etc.)
    SCULPTED,     // Objects with custom sculpted geometry
    MESH,         // Objects using uploaded mesh geometry
    TREE,         // Procedural trees and plants
    GRASS,        // Procedural grass and ground cover
    WATER,        // Water surfaces
    PARTICLE_SYSTEM, // Particle effect emitters
    LIGHT,        // Light sources
    FLEXIBLE,     // Flexible objects (flags, hair, etc.)
    ANIMATED_MESH // Rigged mesh with bone animations
}

/**
 * Object materials affect rendering appearance
 * Based on SecondLife's material system
 */
enum class ObjectMaterial {
    STONE,
    METAL,
    GLASS,
    WOOD,
    FLESH,
    PLASTIC,
    RUBBER,
    LIGHT,  // Emits light
    NONE    // Default material
}

/**
 * Represents terrain in the virtual world
 * Terrain forms the landscape and ground that avatars walk on
 */
data class TerrainPatch(
    val regionX: Int,
    val regionY: Int,
    val patchX: Int, // Patch coordinates within region
    val patchY: Int,
    val heightMap: Array<FloatArray>, // 16x16 height values typically
    val textureIds: List<UUID>, // Ground textures (up to 4 layers)
    val textureScales: List<Float>, // Texture repeat scales
    val lastUpdate: Long = System.currentTimeMillis()
) {
    // Override equals and hashCode for Array handling
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as TerrainPatch
        
        if (regionX != other.regionX) return false
        if (regionY != other.regionY) return false
        if (patchX != other.patchX) return false
        if (patchY != other.patchY) return false
        if (!heightMap.contentDeepEquals(other.heightMap)) return false
        if (textureIds != other.textureIds) return false
        if (textureScales != other.textureScales) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = regionX
        result = 31 * result + regionY
        result = 31 * result + patchX
        result = 31 * result + patchY
        result = 31 * result + heightMap.contentDeepHashCode()
        result = 31 * result + textureIds.hashCode()
        result = 31 * result + textureScales.hashCode()
        return result
    }
}

/**
 * Represents a particle system for visual effects
 * Particle systems create effects like fire, smoke, sparkles, waterfalls, etc.
 */
data class ParticleSystem(
    override val id: UUID,
    override val name: String,
    override val position: Vector3,
    override val rotation: Quaternion,
    override val scale: Vector3,
    override val lastUpdate: Long = System.currentTimeMillis(),
    
    // Particle system properties
    val particleType: ParticleType,
    val emissionRate: Float, // Particles per second
    val particleLifetime: Float, // How long each particle lives
    val startColor: Color,
    val endColor: Color,
    val startSize: Float,
    val endSize: Float,
    val textureId: UUID,
    val isActive: Boolean = true,
    val maxParticles: Int = 1000
) : WorldEntity()

/**
 * Types of particle effects
 */
enum class ParticleType {
    POINT,        // Simple point particles
    TEXTURE,      // Textured billboard particles
    BEAM,         // Beam/laser effects
    RIBBON,       // Ribbon/trail effects
    EXPLOSION,    // Explosion effects
    SMOKE,        // Smoke effects
    FIRE,         // Fire effects
    WATER,        // Water/liquid effects
    SPARKLE       // Sparkle/magic effects
}

/**
 * Simple color representation for particles
 */
data class Color(
    val red: Float,
    val green: Float,
    val blue: Float,
    val alpha: Float = 1.0f
) {
    companion object {
        val WHITE = Color(1.0f, 1.0f, 1.0f)
        val BLACK = Color(0.0f, 0.0f, 0.0f)
        val RED = Color(1.0f, 0.0f, 0.0f)
        val GREEN = Color(0.0f, 1.0f, 0.0f)
        val BLUE = Color(0.0f, 0.0f, 1.0f)
        val YELLOW = Color(1.0f, 1.0f, 0.0f)
        val MAGENTA = Color(1.0f, 0.0f, 1.0f)
        val CYAN = Color(0.0f, 1.0f, 1.0f)
    }
}

/**
 * Utility functions for working with world entities
 */
object WorldEntityUtils {
    
    /**
     * Calculate distance between two world entities
     */
    fun distance(entity1: WorldEntity, entity2: WorldEntity): Float {
        val dx = entity1.position.x - entity2.position.x
        val dy = entity1.position.y - entity2.position.y
        val dz = entity1.position.z - entity2.position.z
        return kotlin.math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
    }
    
    /**
     * Check if an entity is within a given radius of a position
     */
    fun isWithinRadius(entity: WorldEntity, center: Vector3, radius: Float): Boolean {
        val dx = entity.position.x - center.x
        val dy = entity.position.y - center.y
        val dz = entity.position.z - center.z
        val distanceSquared = dx * dx + dy * dy + dz * dz
        return distanceSquared <= radius * radius
    }
    
    /**
     * Create a simple cube object for demonstrations
     */
    fun createDemoCube(position: Vector3, name: String = "Demo Cube"): VirtualObject {
        return VirtualObject(
            id = UUID.randomUUID(),
            name = name,
            position = position,
            rotation = Quaternion(0f, 0f, 0f, 1f),
            scale = Vector3(1f, 1f, 1f),
            description = "A demonstration cube object",
            creatorId = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            objectType = ObjectType.PRIMITIVE,
            material = ObjectMaterial.STONE,
            textureIds = listOf(UUID.randomUUID()) // Random texture ID
        )
    }
    
    /**
     * Create a demo avatar for testing
     */
    fun createDemoAvatar(position: Vector3, name: String = "Demo Avatar"): Avatar {
        return Avatar(
            id = UUID.randomUUID(),
            name = name,
            position = position,
            rotation = Quaternion(0f, 0f, 0f, 1f),
            displayName = name,
            username = name.lowercase().replace(" ", "."),
            appearanceHash = "demo-appearance-${System.currentTimeMillis()}",
            animationState = AnimationState(
                currentAnimation = "stand",
                looping = true
            ),
            attachments = emptyList()
        )
    }
}