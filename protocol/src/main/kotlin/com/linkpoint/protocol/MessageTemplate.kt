package com.linkpoint.protocol

/**
 * Message Template System for SecondLife Protocol
 * 
 * This implements the message template system used by SecondLife/OpenSim
 * to define the structure and serialization of UDP messages.
 * 
 * Based on message_template.msg from SecondLife viewer source.
 */

/**
 * Core message types used in SecondLife protocol
 */
enum class MessageTemplate(
    val id: Int,
    val name: String,
    val frequency: MessageFrequency,
    val trust: MessageTrust,
    val encoding: MessageEncoding
) {
    // Authentication messages
    USE_CIRCUIT_CODE(1, "UseCircuitCode", MessageFrequency.HIGH, MessageTrust.TRUSTED, MessageEncoding.UNENCODED),
    COMPLETE_AGENT_MOVEMENT(2, "CompleteAgentMovement", MessageFrequency.HIGH, MessageTrust.TRUSTED, MessageEncoding.UNENCODED),
    
    // Agent messages
    AGENT_UPDATE(3, "AgentUpdate", MessageFrequency.FIXED, MessageTrust.TRUSTED, MessageEncoding.UNENCODED),
    AGENT_ANIMATION(4, "AgentAnimation", MessageFrequency.HIGH, MessageTrust.TRUSTED, MessageEncoding.UNENCODED),
    
    // Chat messages
    CHAT_FROM_VIEWER(5, "ChatFromViewer", MessageFrequency.LOW, MessageTrust.TRUSTED, MessageEncoding.UNENCODED),
    CHAT_FROM_SIMULATOR(6, "ChatFromSimulator", MessageFrequency.LOW, MessageTrust.TRUSTED, MessageEncoding.UNENCODED),
    
    // Object messages
    OBJECT_UPDATE(7, "ObjectUpdate", MessageFrequency.MEDIUM, MessageTrust.TRUSTED, MessageEncoding.UNENCODED),
    OBJECT_UPDATE_COMPRESSED(8, "ObjectUpdateCompressed", MessageFrequency.MEDIUM, MessageTrust.TRUSTED, MessageEncoding.UNENCODED),
    
    // Asset messages
    REQUEST_IMAGE(9, "RequestImage", MessageFrequency.HIGH, MessageTrust.TRUSTED, MessageEncoding.UNENCODED),
    IMAGE_DATA(10, "ImageData", MessageFrequency.HIGH, MessageTrust.TRUSTED, MessageEncoding.UNENCODED),
    
    // Network management
    PACKET_ACK(11, "PacketAck", MessageFrequency.FIXED, MessageTrust.TRUSTED, MessageEncoding.UNENCODED),
    PING_PONG(12, "PingPong", MessageFrequency.FIXED, MessageTrust.TRUSTED, MessageEncoding.UNENCODED);
    
    companion object {
        private val idMap = values().associateBy { it.id }
        private val nameMap = values().associateBy { it.name }
        
        fun fromId(id: Int): MessageTemplate? = idMap[id]
        fun fromName(name: String): MessageTemplate? = nameMap[name]
    }
}

/**
 * Message frequency categories from SecondLife protocol
 */
enum class MessageFrequency {
    FIXED,    // Fixed frequency (0)
    HIGH,     // High frequency (1) 
    MEDIUM,   // Medium frequency (2)
    LOW       // Low frequency (3)
}

/**
 * Message trust levels
 */
enum class MessageTrust {
    TRUSTED,      // Trusted message
    NOT_TRUSTED   // Not trusted message
}

/**
 * Message encoding types
 */
enum class MessageEncoding {
    UNENCODED,    // Unencoded message
    ENCODED       // Encoded message
}

/**
 * Message field types from SecondLife protocol
 */
enum class MessageFieldType(val size: Int) {
    // Fixed size types
    U8(1),        // Unsigned 8-bit integer
    U16(2),       // Unsigned 16-bit integer
    U32(4),       // Unsigned 32-bit integer
    U64(8),       // Unsigned 64-bit integer
    S8(1),        // Signed 8-bit integer
    S16(2),       // Signed 16-bit integer
    S32(4),       // Signed 32-bit integer
    S64(8),       // Signed 64-bit integer
    F32(4),       // 32-bit float
    F64(8),       // 64-bit float
    BOOL(1),      // Boolean (1 byte)
    UUID(16),     // 128-bit UUID
    
    // Variable size types
    VARIABLE(-1), // Variable length field
    FIXED(-2);    // Fixed but template-defined length
}

/**
 * Base class for message structures
 */
abstract class MessageStructure {
    abstract val template: MessageTemplate
    abstract fun serialize(): ByteArray
    abstract fun deserialize(data: ByteArray): Boolean
}

/**
 * UseCircuitCode message structure
 */
data class UseCircuitCodeMessage(
    val code: Int,
    val sessionId: String,
    val agentId: String
) : MessageStructure() {
    
    override val template = MessageTemplate.USE_CIRCUIT_CODE
    
    override fun serialize(): ByteArray {
        // TODO: Implement proper serialization
        return byteArrayOf()
    }
    
    override fun deserialize(data: ByteArray): Boolean {
        // TODO: Implement proper deserialization
        return false
    }
}

/**
 * AgentUpdate message structure (high frequency, sent ~20 times per second)
 */
data class AgentUpdateMessage(
    val agentId: String,
    val sessionId: String,
    val bodyRotation: List<Float>, // Quaternion [x,y,z,w]
    val headRotation: List<Float>, // Quaternion [x,y,z,w] 
    val state: Int,
    val cameraCenter: List<Float>, // Vector3 [x,y,z]
    val cameraAtAxis: List<Float>, // Vector3 [x,y,z]
    val cameraLeftAxis: List<Float>, // Vector3 [x,y,z]
    val cameraUpAxis: List<Float>, // Vector3 [x,y,z]
    val far: Float,
    val controlFlags: Int,
    val flags: Int
) : MessageStructure() {
    
    override val template = MessageTemplate.AGENT_UPDATE
    
    override fun serialize(): ByteArray {
        // TODO: Implement proper serialization
        return byteArrayOf()
    }
    
    override fun deserialize(data: ByteArray): Boolean {
        // TODO: Implement proper deserialization  
        return false
    }
}

/**
 * ChatFromViewer message structure
 */
data class ChatFromViewerMessage(
    val agentId: String,
    val sessionId: String,
    val message: String,
    val type: Int,
    val channel: Int
) : MessageStructure() {
    
    override val template = MessageTemplate.CHAT_FROM_VIEWER
    
    override fun serialize(): ByteArray {
        // TODO: Implement proper serialization
        return byteArrayOf()
    }
    
    override fun deserialize(data: ByteArray): Boolean {
        // TODO: Implement proper deserialization
        return false
    }
}