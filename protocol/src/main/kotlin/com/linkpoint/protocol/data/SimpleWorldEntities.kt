package com.linkpoint.protocol.data

import java.util.UUID

/**
 * Simplified World Entity Data Structures (no external dependencies)
 * 
 * These classes model the core entities that exist in virtual worlds, imported and modernized
 * from the SecondLife viewer's object and avatar representation for demonstration purposes.
 */

// Basic 3D math classes for the demo
data class SimpleVector3(val x: Float, val y: Float, val z: Float) {
    override fun toString(): String = "(%.1f, %.1f, %.1f)".format(x, y, z)
}

data class SimpleQuaternion(val x: Float, val y: Float, val z: Float, val w: Float) {
    override fun toString(): String = "(%.2f, %.2f, %.2f, %.2f)".format(x, y, z, w)
}

data class SimpleColor(val red: Float, val green: Float, val blue: Float, val alpha: Float = 1.0f) {
    companion object {
        val WHITE = SimpleColor(1.0f, 1.0f, 1.0f)
        val BLACK = SimpleColor(0.0f, 0.0f, 0.0f)
        val RED = SimpleColor(1.0f, 0.0f, 0.0f)
        val GREEN = SimpleColor(0.0f, 1.0f, 0.0f)
        val BLUE = SimpleColor(0.0f, 0.0f, 1.0f)
        val YELLOW = SimpleColor(1.0f, 1.0f, 0.0f)
    }
}

/**
 * Base class for all entities that exist in the virtual world
 */
sealed class SimpleWorldEntity {
    abstract val id: UUID
    abstract val name: String
    abstract val position: SimpleVector3
    abstract val rotation: SimpleQuaternion
    abstract val scale: SimpleVector3
    abstract val lastUpdate: Long
}

/**
 * Represents an avatar (user character) in the virtual world
 */
data class SimpleAvatar(
    override val id: UUID,
    override val name: String,
    override val position: SimpleVector3,
    override val rotation: SimpleQuaternion,
    override val scale: SimpleVector3 = SimpleVector3(1.0f, 1.0f, 1.0f),
    override val lastUpdate: Long = System.currentTimeMillis(),
    
    val displayName: String,
    val username: String,
    val animationState: String,
    val health: Float = 100.0f,
    val energy: Float = 100.0f
) : SimpleWorldEntity()

/**
 * Represents a virtual object (prim) in the world
 */
data class SimpleVirtualObject(
    override val id: UUID,
    override val name: String,
    override val position: SimpleVector3,
    override val rotation: SimpleQuaternion,
    override val scale: SimpleVector3,
    override val lastUpdate: Long = System.currentTimeMillis(),
    
    val description: String,
    val objectType: SimpleObjectType,
    val material: SimpleObjectMaterial,
    val isScripted: Boolean = false
) : SimpleWorldEntity()

/**
 * Represents a particle system for visual effects
 */
data class SimpleParticleSystem(
    override val id: UUID,
    override val name: String,
    override val position: SimpleVector3,
    override val rotation: SimpleQuaternion,
    override val scale: SimpleVector3,
    override val lastUpdate: Long = System.currentTimeMillis(),
    
    val particleType: SimpleParticleType,
    val emissionRate: Float,
    val particleLifetime: Float,
    val startColor: SimpleColor,
    val endColor: SimpleColor,
    val startSize: Float,
    val endSize: Float,
    val isActive: Boolean = true
) : SimpleWorldEntity()

enum class SimpleObjectType {
    PRIMITIVE, SCULPTED, MESH, TREE, GRASS, WATER, PARTICLE_SYSTEM, LIGHT, FLEXIBLE, ANIMATED_MESH
}

enum class SimpleObjectMaterial {
    STONE, METAL, GLASS, WOOD, FLESH, PLASTIC, RUBBER, LIGHT, NONE
}

enum class SimpleParticleType {
    POINT, TEXTURE, BEAM, RIBBON, EXPLOSION, SMOKE, FIRE, WATER, SPARKLE
}

/**
 * Utility functions for working with world entities
 */
object SimpleWorldEntityUtils {
    
    fun distance(entity1: SimpleWorldEntity, entity2: SimpleWorldEntity): Float {
        val dx = entity1.position.x - entity2.position.x
        val dy = entity1.position.y - entity2.position.y
        val dz = entity1.position.z - entity2.position.z
        return kotlin.math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
    }
}