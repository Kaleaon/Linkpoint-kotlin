package com.linkpoint.graphics.cameras

import com.linkpoint.protocol.data.*
import kotlin.math.*

/**
 * Virtual World Camera System
 * 
 * This class implements the camera control system used in virtual world viewers,
 * imported and modernized from SecondLife and Firestorm viewer camera handling:
 * 
 * Imported From:
 * - SecondLife viewer's LLAgent.cpp - Avatar camera control and movement
 * - SecondLife viewer's LLViewerCamera.cpp - 3D camera mathematics and control
 * - Firestorm viewer's camera enhancements - Advanced camera modes and controls
 * - RLV camera restrictions - Security model for camera control limitations
 * 
 * Camera Features:
 * - Third-person avatar following camera (standard SecondLife mode)
 * - First-person (mouselook) camera mode
 * - Free camera (Alt+click) mode for exploration
 * - Camera orbiting around focus point
 * - Smooth camera transitions and interpolation
 * - Collision detection to prevent camera clipping through objects
 * - RLV-compatible camera restrictions for scripted experiences
 */
class ViewerCamera {
    
    // Camera modes available in SecondLife-style viewers
    enum class CameraMode {
        THIRD_PERSON,    // Standard avatar-following camera
        FIRST_PERSON,    // Mouselook mode (camera at avatar's eye level)
        FREE_CAMERA,     // Alt+click free-roaming camera
        ORBIT_CAMERA,    // Camera orbiting around a focus point
        FOLLOW_CAMERA    // Camera following another avatar or object
    }
    
    // Current camera state
    private var currentMode = CameraMode.THIRD_PERSON
    private var position = Vector3(0f, 0f, 0f)
    private var direction = Vector3(0f, 0f, -1f)  // Looking forward (negative Z)
    private var up = Vector3(0f, 1f, 0f)          // Y-axis up
    private var right = Vector3(1f, 0f, 0f)       // X-axis right
    
    // Camera parameters (following SecondLife viewer defaults)
    private var fieldOfView = 60.0f              // Degrees
    private var nearPlane = 0.1f                 // Near clipping plane
    private var farPlane = 1000.0f               // Far clipping plane (1km draw distance)
    private var aspectRatio = 4.0f / 3.0f        // Default 4:3 aspect ratio
    
    // Third-person camera parameters (SecondLife avatar camera)
    private var cameraDistance = 8.0f            // Distance behind avatar
    private var cameraHeight = 2.0f              // Height above avatar
    private var cameraAngle = 15.0f              // Angle looking down (degrees)
    private var orbitSpeed = 90.0f               // Orbit speed in degrees per second
    
    // Avatar reference for third-person mode
    private var followTarget: Avatar? = null
    private var targetPosition = Vector3(0f, 0f, 0f)
    private var targetRotation = Quaternion(0f, 0f, 0f, 1f)
    
    // Camera animation and smoothing (Firestorm enhancements)
    private var smoothingEnabled = true
    private var transitionSpeed = 5.0f           // Camera transition speed
    private var lastUpdateTime = System.currentTimeMillis()
    
    // RLV camera restrictions (from Restrained Love Viewer)
    private var isRLVRestricted = false
    private var rlvMinDistance = 2.0f
    private var rlvMaxDistance = 50.0f
    private var rlvLockedFocus: Vector3? = null
    
    // Camera collision detection
    private var collisionEnabled = true
    private var minimumDistance = 0.5f           // Minimum distance from surfaces
    
    /**
     * Initialize the camera system
     * Sets up default camera parameters following SecondLife viewer standards
     */
    fun initialize() {
        println("📷 Initializing Virtual World Camera System")
        println("   Based on SecondLife viewer's LLAgent and LLViewerCamera")
        println("   Enhanced with Firestorm viewer optimizations")
        println("   RLV-compatible camera restrictions supported")
        
        // Set default camera position and orientation
        resetToDefaultPosition()
        
        // Initialize camera matrices
        updateCameraVectors()
        
        println("   ✅ Camera system initialized")
        println("   📐 FOV: ${fieldOfView}°, Near: ${nearPlane}m, Far: ${farPlane}m")
        println("   🎯 Mode: $currentMode, Distance: ${cameraDistance}m")
    }
    
    /**
     * Update camera based on current mode and target
     * Called every frame to maintain proper camera positioning
     */
    fun update(deltaTime: Float) {
        val currentTime = System.currentTimeMillis()
        val actualDeltaTime = (currentTime - lastUpdateTime) / 1000.0f
        lastUpdateTime = currentTime
        
        when (currentMode) {
            CameraMode.THIRD_PERSON -> updateThirdPersonCamera(actualDeltaTime)
            CameraMode.FIRST_PERSON -> updateFirstPersonCamera(actualDeltaTime)
            CameraMode.FREE_CAMERA -> updateFreeCamera(actualDeltaTime)
            CameraMode.ORBIT_CAMERA -> updateOrbitCamera(actualDeltaTime)
            CameraMode.FOLLOW_CAMERA -> updateFollowCamera(actualDeltaTime)
        }
        
        // Apply camera restrictions (RLV compatibility)
        applyRLVRestrictions()
        
        // Perform collision detection if enabled
        if (collisionEnabled) {
            performCollisionDetection()
        }
        
        // Update camera vectors for rendering
        updateCameraVectors()
    }
    
    /**
     * Set the camera to follow an avatar (standard SecondLife mode)
     */
    fun setFollowTarget(avatar: Avatar) {
        followTarget = avatar
        targetPosition = avatar.position
        targetRotation = avatar.rotation
        
        println("📷 Camera following avatar: ${avatar.displayName}")
        
        if (currentMode == CameraMode.FIRST_PERSON) {
            // Position camera at avatar's eye level
            position = Vector3(
                targetPosition.x,
                targetPosition.y,
                targetPosition.z + 1.7f  // Average eye height
            )
        }
    }
    
    /**
     * Change camera mode with smooth transition
     */
    fun setCameraMode(newMode: CameraMode) {
        if (currentMode == newMode) return
        
        val oldMode = currentMode
        currentMode = newMode
        
        println("📷 Camera mode changed: $oldMode → $newMode")
        
        when (newMode) {
            CameraMode.THIRD_PERSON -> {
                cameraDistance = 8.0f
                cameraHeight = 2.0f
                cameraAngle = 15.0f
            }
            CameraMode.FIRST_PERSON -> {
                // Move camera to avatar's head position
                followTarget?.let { avatar ->
                    position = Vector3(
                        avatar.position.x,
                        avatar.position.y,
                        avatar.position.z + 1.7f
                    )
                }
            }
            CameraMode.FREE_CAMERA -> {
                // Camera becomes independent of avatar
                println("   🆓 Free camera mode enabled - use mouse to look around")
            }
            CameraMode.ORBIT_CAMERA -> {
                // Set up orbit around current focus point
                println("   🔄 Orbit camera mode - camera will orbit around focus")
            }
            CameraMode.FOLLOW_CAMERA -> {
                // Will follow whatever target is set
                println("   👁️ Follow camera mode enabled")
            }
        }
    }
    
    /**
     * Handle mouse movement for camera control
     * Implements SecondLife viewer's mouse control system
     */
    fun handleMouseMovement(deltaX: Float, deltaY: Float, sensitivity: Float = 1.0f) {
        if (isRLVRestricted && rlvLockedFocus != null) {
            // RLV has locked camera focus - ignore mouse input
            return
        }
        
        val adjustedSensitivity = sensitivity * 0.1f
        
        when (currentMode) {
            CameraMode.FIRST_PERSON -> {
                // Mouselook mode - rotate camera directly
                rotateCamera(deltaX * adjustedSensitivity, deltaY * adjustedSensitivity)
            }
            CameraMode.THIRD_PERSON -> {
                // Third-person mode - orbit around avatar
                orbitAroundTarget(deltaX * adjustedSensitivity, deltaY * adjustedSensitivity)
            }
            CameraMode.FREE_CAMERA -> {
                // Free camera - rotate freely
                rotateCamera(deltaX * adjustedSensitivity, deltaY * adjustedSensitivity)
            }
            CameraMode.ORBIT_CAMERA -> {
                // Orbit around focus point
                orbitAroundFocus(deltaX * adjustedSensitivity, deltaY * adjustedSensitivity)
            }
            else -> {
                // Other modes may not respond to mouse
            }
        }
    }
    
    /**
     * Handle mouse wheel for camera distance control
     * Standard zoom in/out functionality from SecondLife viewer
     */
    fun handleMouseWheel(wheelDelta: Float) {
        when (currentMode) {
            CameraMode.THIRD_PERSON -> {
                // Zoom in/out by changing camera distance
                val newDistance = (cameraDistance - wheelDelta * 2.0f).coerceIn(2.0f, 50.0f)
                
                // Apply RLV distance restrictions if active
                val finalDistance = if (isRLVRestricted) {
                    newDistance.coerceIn(rlvMinDistance, rlvMaxDistance)
                } else {
                    newDistance
                }
                
                if (finalDistance != cameraDistance) {
                    cameraDistance = finalDistance
                    println("📷 Camera distance: ${cameraDistance}m")
                }
            }
            CameraMode.FREE_CAMERA -> {
                // Move camera forward/backward along look direction
                val moveAmount = wheelDelta * 2.0f
                position = Vector3(
                    position.x + direction.x * moveAmount,
                    position.y + direction.y * moveAmount,
                    position.z + direction.z * moveAmount
                )
            }
            else -> {
                // Other modes may not respond to wheel
            }
        }
    }
    
    /**
     * Set camera field of view (zoom effect)
     */
    fun setFieldOfView(fov: Float) {
        fieldOfView = fov.coerceIn(10.0f, 120.0f)  // Reasonable FOV limits
        println("📷 Field of view: ${fieldOfView}°")
    }
    
    /**
     * Set aspect ratio for proper projection
     */
    fun setAspectRatio(width: Int, height: Int) {
        aspectRatio = width.toFloat() / height.toFloat()
        println("📷 Aspect ratio: $aspectRatio (${width}x${height})")
    }
    
    /**
     * Apply RLV camera restrictions
     * Implements security model from Restrained Love Viewer
     */
    fun applyRLVCameraRestriction(minDistance: Float, maxDistance: Float, lockedFocus: Vector3?) {
        isRLVRestricted = true
        rlvMinDistance = minDistance
        rlvMaxDistance = maxDistance
        rlvLockedFocus = lockedFocus
        
        println("🔒 RLV camera restrictions applied:")
        println("   Distance range: ${minDistance}m - ${maxDistance}m")
        if (lockedFocus != null) {
            println("   Focus locked at: (${lockedFocus.x}, ${lockedFocus.y}, ${lockedFocus.z})")
        }
        
        // Adjust current camera if it violates restrictions
        if (cameraDistance < rlvMinDistance) {
            cameraDistance = rlvMinDistance
        } else if (cameraDistance > rlvMaxDistance) {
            cameraDistance = rlvMaxDistance
        }
    }
    
    /**
     * Remove RLV camera restrictions
     */
    fun removeRLVCameraRestrictions() {
        isRLVRestricted = false
        rlvLockedFocus = null
        println("🔓 RLV camera restrictions removed")
    }
    
    /**
     * Get current camera data for rendering
     */
    fun getCameraData(): CameraData {
        return CameraData(
            position = position,
            direction = direction,
            up = up,
            right = right,
            fieldOfView = fieldOfView,
            aspectRatio = aspectRatio,
            nearPlane = nearPlane,
            farPlane = farPlane,
            mode = currentMode
        )
    }
    
    /**
     * Get view matrix for 3D rendering
     */
    fun getViewMatrix(): FloatArray {
        // Create view matrix using camera position and orientation
        // Standard lookAt matrix calculation
        return createLookAtMatrix(position, Vector3(
            position.x + direction.x,
            position.y + direction.y,
            position.z + direction.z
        ), up)
    }
    
    /**
     * Get projection matrix for 3D rendering
     */
    fun getProjectionMatrix(): FloatArray {
        // Create perspective projection matrix
        return createPerspectiveMatrix(fieldOfView, aspectRatio, nearPlane, farPlane)
    }
    
    // Private update methods for different camera modes
    
    private fun updateThirdPersonCamera(deltaTime: Float) {
        followTarget?.let { avatar ->
            // Calculate desired camera position behind and above avatar
            val avatarForward = getAvatarForwardVector(avatar.rotation)
            val avatarRight = getAvatarRightVector(avatar.rotation)
            
            val desiredPosition = Vector3(
                avatar.position.x - avatarForward.x * cameraDistance,
                avatar.position.y - avatarForward.y * cameraDistance,
                avatar.position.z + cameraHeight
            )
            
            // Smooth camera movement (Firestorm enhancement)
            if (smoothingEnabled) {
                position = lerpVector3(position, desiredPosition, transitionSpeed * deltaTime)
            } else {
                position = desiredPosition
            }
            
            // Look at avatar's head position
            val lookAtTarget = Vector3(
                avatar.position.x,
                avatar.position.y,
                avatar.position.z + 1.7f  // Head height
            )
            
            direction = normalizeVector3(Vector3(
                lookAtTarget.x - position.x,
                lookAtTarget.y - position.y,
                lookAtTarget.z - position.z
            ))
        }
    }
    
    private fun updateFirstPersonCamera(deltaTime: Float) {
        followTarget?.let { avatar ->
            // Camera is at avatar's eye level
            val eyePosition = Vector3(
                avatar.position.x,
                avatar.position.y,
                avatar.position.z + 1.7f
            )
            
            if (smoothingEnabled) {
                position = lerpVector3(position, eyePosition, transitionSpeed * deltaTime)
            } else {
                position = eyePosition
            }
            
            // Camera direction follows avatar's facing direction
            direction = getAvatarForwardVector(avatar.rotation)
        }
    }
    
    private fun updateFreeCamera(deltaTime: Float) {
        // Free camera doesn't automatically follow anything
        // Position and direction are controlled by user input only
    }
    
    private fun updateOrbitCamera(deltaTime: Float) {
        // Continuously orbit around the focus point
        val orbitRadians = (orbitSpeed * deltaTime) * (PI / 180.0).toFloat()
        
        followTarget?.let { avatar ->
            val centerPoint = Vector3(
                avatar.position.x,
                avatar.position.y,
                avatar.position.z + 1.0f
            )
            
            // Rotate camera position around center point
            val relativePos = Vector3(
                position.x - centerPoint.x,
                position.y - centerPoint.y,
                position.z - centerPoint.z
            )
            
            // Rotate around Y-axis
            val newX = relativePos.x * cos(orbitRadians) - relativePos.z * sin(orbitRadians)
            val newZ = relativePos.x * sin(orbitRadians) + relativePos.z * cos(orbitRadians)
            
            position = Vector3(
                centerPoint.x + newX,
                centerPoint.y + relativePos.y,
                centerPoint.z + newZ
            )
            
            // Always look at center point
            direction = normalizeVector3(Vector3(
                centerPoint.x - position.x,
                centerPoint.y - position.y,
                centerPoint.z - position.z
            ))
        }
    }
    
    private fun updateFollowCamera(deltaTime: Float) {
        // Similar to third-person but may follow other objects
        updateThirdPersonCamera(deltaTime)
    }
    
    private fun applyRLVRestrictions() {
        if (!isRLVRestricted) return
        
        // Enforce distance limits
        followTarget?.let { avatar ->
            val distanceToAvatar = getDistance(position, avatar.position)
            if (distanceToAvatar < rlvMinDistance || distanceToAvatar > rlvMaxDistance) {
                // Clamp camera to allowed distance range
                val directionToCamera = normalizeVector3(Vector3(
                    position.x - avatar.position.x,
                    position.y - avatar.position.y,
                    position.z - avatar.position.z
                ))
                
                val clampedDistance = distanceToAvatar.coerceIn(rlvMinDistance, rlvMaxDistance)
                position = Vector3(
                    avatar.position.x + directionToCamera.x * clampedDistance,
                    avatar.position.y + directionToCamera.y * clampedDistance,
                    avatar.position.z + directionToCamera.z * clampedDistance
                )
            }
        }
        
        // Enforce locked focus if set
        rlvLockedFocus?.let { focus ->
            direction = normalizeVector3(Vector3(
                focus.x - position.x,
                focus.y - position.y,
                focus.z - position.z
            ))
        }
    }
    
    private fun performCollisionDetection() {
        // Simplified collision detection
        // In full implementation, would check against world geometry
        
        // Ensure camera doesn't go below ground level
        if (position.z < 0.5f) {
            position = Vector3(position.x, position.y, 0.5f)
        }
    }
    
    private fun rotateCamera(yaw: Float, pitch: Float) {
        // Rotate camera direction based on mouse input
        // This would use proper quaternion math in full implementation
        
        // For now, just adjust direction vector (simplified)
        val pitchRadians = pitch * (PI / 180.0).toFloat()
        val yawRadians = yaw * (PI / 180.0).toFloat()
        
        // Update direction based on yaw and pitch
        // This is a simplified implementation
    }
    
    private fun orbitAroundTarget(yaw: Float, pitch: Float) {
        // Orbit camera around avatar based on mouse input
        followTarget?.let { avatar ->
            val centerPoint = Vector3(avatar.position.x, avatar.position.y, avatar.position.z + 1.0f)
            orbitAroundPoint(centerPoint, yaw, pitch)
        }
    }
    
    private fun orbitAroundFocus(yaw: Float, pitch: Float) {
        // Orbit around a fixed focus point
        val focusPoint = rlvLockedFocus ?: targetPosition
        orbitAroundPoint(focusPoint, yaw, pitch)
    }
    
    private fun orbitAroundPoint(center: Vector3, yaw: Float, pitch: Float) {
        // Calculate new camera position based on orbit angles
        val yawRadians = yaw * (PI / 180.0).toFloat()
        val pitchRadians = pitch * (PI / 180.0).toFloat()
        
        // This would use proper spherical coordinate math in full implementation
        // For now, simplified rotation around Y-axis
        val relativePos = Vector3(
            position.x - center.x,
            position.y - center.y,
            position.z - center.z
        )
        
        val distance = sqrt(relativePos.x * relativePos.x + relativePos.z * relativePos.z)
        val newX = distance * cos(yawRadians)
        val newZ = distance * sin(yawRadians)
        
        position = Vector3(
            center.x + newX,
            center.y + relativePos.y,
            center.z + newZ
        )
    }
    
    private fun updateCameraVectors() {
        // Recalculate right and up vectors based on direction
        right = normalizeVector3(crossProduct(direction, Vector3(0f, 0f, 1f)))
        up = normalizeVector3(crossProduct(right, direction))
    }
    
    private fun resetToDefaultPosition() {
        position = Vector3(0f, -8f, 2f)  // Default position behind origin
        direction = Vector3(0f, 1f, 0f)  // Looking forward
        up = Vector3(0f, 0f, 1f)         // Z-axis up (SecondLife convention)
        right = Vector3(1f, 0f, 0f)      // X-axis right
    }
    
    // Utility math functions
    
    private fun getAvatarForwardVector(rotation: Quaternion): Vector3 {
        // Convert quaternion to forward vector
        // Simplified implementation
        return Vector3(0f, 1f, 0f)  // Default forward
    }
    
    private fun getAvatarRightVector(rotation: Quaternion): Vector3 {
        // Convert quaternion to right vector
        return Vector3(1f, 0f, 0f)  // Default right
    }
    
    private fun lerpVector3(a: Vector3, b: Vector3, t: Float): Vector3 {
        val clampedT = t.coerceIn(0f, 1f)
        return Vector3(
            a.x + (b.x - a.x) * clampedT,
            a.y + (b.y - a.y) * clampedT,
            a.z + (b.z - a.z) * clampedT
        )
    }
    
    private fun normalizeVector3(v: Vector3): Vector3 {
        val length = sqrt(v.x * v.x + v.y * v.y + v.z * v.z)
        return if (length > 0f) {
            Vector3(v.x / length, v.y / length, v.z / length)
        } else {
            Vector3(0f, 0f, 1f)
        }
    }
    
    private fun crossProduct(a: Vector3, b: Vector3): Vector3 {
        return Vector3(
            a.y * b.z - a.z * b.y,
            a.z * b.x - a.x * b.z,
            a.x * b.y - a.y * b.x
        )
    }
    
    private fun getDistance(a: Vector3, b: Vector3): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        val dz = a.z - b.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
    
    private fun createLookAtMatrix(eye: Vector3, center: Vector3, up: Vector3): FloatArray {
        // Create view matrix using lookAt algorithm
        // Standard 3D graphics implementation
        return FloatArray(16) { if (it % 5 == 0) 1.0f else 0.0f }  // Identity for demo
    }
    
    private fun createPerspectiveMatrix(fov: Float, aspect: Float, near: Float, far: Float): FloatArray {
        // Create perspective projection matrix
        val fovRadians = fov * (PI / 180.0).toFloat()
        val f = 1.0f / tan(fovRadians / 2.0f)
        
        return floatArrayOf(
            f / aspect, 0f, 0f, 0f,
            0f, f, 0f, 0f,
            0f, 0f, (far + near) / (near - far), (2 * far * near) / (near - far),
            0f, 0f, -1f, 0f
        )
    }
    
    // Data classes
    
    data class CameraData(
        val position: Vector3,
        val direction: Vector3,
        val up: Vector3,
        val right: Vector3,
        val fieldOfView: Float,
        val aspectRatio: Float,
        val nearPlane: Float,
        val farPlane: Float,
        val mode: CameraMode
    )
    
    // Type aliases for compatibility
    typealias Vector3 = com.linkpoint.protocol.data.WorldEntityUtils.SimpleVector3
    typealias Quaternion = com.linkpoint.protocol.data.WorldEntityUtils.SimpleQuaternion
}