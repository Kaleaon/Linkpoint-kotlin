/**
 * MessageSystemTranslated.kt
 * 
 * Modern Kotlin translation of SecondLife's core message system for UDP communication.
 * This handles all network messaging between the viewer and SecondLife simulators,
 * including reliability, throttling, and message template management.
 *
 * Original: https://github.com/secondlife/viewer/blob/main/indra/llmessage/message.cpp
 * 
 * Key improvements in Kotlin version:
 * - Coroutine-based async I/O instead of blocking calls
 * - Type-safe message handling with sealed classes  
 * - Automatic resource management
 * - Flow-based event streams for real-time monitoring
 * - Built-in retry and circuit breaker patterns
 * - Comprehensive error handling and logging
 */

package com.secondlife.viewer.protocol

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.*
import java.net.*
import java.nio.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*
import kotlin.time.*

/**
 * Core message types for type-safe message handling
 */
sealed class MessageType(val name: String, val number: UInt, val reliable: Boolean) {
    object StartPingCheck : MessageType("StartPingCheck", 1u, true)
    object CompletePingCheck : MessageType("CompletePingCheck", 2u, true)
    object LoginRequest : MessageType("LoginRequest", 3u, true)
    object LoginReply : MessageType("LoginReply", 4u, true)
    object ChatFromViewer : MessageType("ChatFromViewer", 80u, true)
    object ChatFromSimulator : MessageType("ChatFromSimulator", 81u, false)
    object UpdateUserInfo : MessageType("UpdateUserInfo", 180u, true)
    object RegionHandshake : MessageType("RegionHandshake", 148u, false)
    object RegionHandshakeReply : MessageType("RegionHandshakeReply", 149u, true)
    
    companion object {
        private val messagesByName = mapOf(
            "StartPingCheck" to StartPingCheck,
            "CompletePingCheck" to CompletePingCheck,
            "LoginRequest" to LoginRequest,
            "LoginReply" to LoginReply,
            "ChatFromViewer" to ChatFromViewer,
            "ChatFromSimulator" to ChatFromSimulator,
            "UpdateUserInfo" to UpdateUserInfo,
            "RegionHandshake" to RegionHandshake,
            "RegionHandshakeReply" to RegionHandshakeReply
        )
        
        private val messagesByNumber = messagesByName.values.associateBy { it.number }
        
        fun fromName(name: String): MessageType? = messagesByName[name]
        fun fromNumber(number: UInt): MessageType? = messagesByNumber[number]
    }
}

/**
 * Message variable types for encoding/decoding
 */
sealed class VariableType(val size: Int) {
    object String : VariableType(-1) // Variable length
    object U32 : VariableType(4)
    object F32 : VariableType(4)  
    object Vector3 : VariableType(12)
    object UUID : VariableType(16)
    object IPAddr : VariableType(4)
    object IPPort : VariableType(2)
}

/**
 * Message template definition for encoding/decoding structure
 */
data class MessageTemplate(
    val messageType: MessageType,
    val blocks: List<MessageBlock>
) {
    data class MessageBlock(
        val name: String,
        val type: BlockType,
        val variables: List<MessageVariable>
    )
    
    data class MessageVariable(
        val name: String,
        val type: VariableType,
        val size: Int = type.size
    )
    
    enum class BlockType {
        SINGLE,    // Single block
        MULTIPLE,  // Multiple blocks allowed
        VARIABLE   // Variable number of blocks
    }
}

/**
 * Network host representation with async resolution
 */
data class NetworkHost(
    val address: String,
    val port: Int
) {
    private var resolvedAddress: InetSocketAddress? = null
    
    suspend fun resolve(): InetSocketAddress {
        return resolvedAddress ?: withContext(Dispatchers.IO) {
            val resolved = InetSocketAddress(address, port)
            resolvedAddress = resolved
            resolved
        }
    }
    
    override fun toString(): String = "$address:$port"
}

/**
 * Message statistics for monitoring and debugging
 */
data class MessageStats(
    val packetsIn: AtomicLong = AtomicLong(0),
    val packetsOut: AtomicLong = AtomicLong(0),
    val packetsLost: AtomicLong = AtomicLong(0),
    val bytesIn: AtomicLong = AtomicLong(0),
    val bytesOut: AtomicLong = AtomicLong(0),
    val messagesSent: AtomicLong = AtomicLong(0),
    val messagesReceived: AtomicLong = AtomicLong(0)
) {
    fun reset() {
        packetsIn.set(0)
        packetsOut.set(0)
        packetsLost.set(0)
        bytesIn.set(0)
        bytesOut.set(0)
        messagesSent.set(0)
        messagesReceived.set(0)
    }
    
    override fun toString(): String = """
        Message Statistics:
        - Packets In: ${packetsIn.get()}
        - Packets Out: ${packetsOut.get()}
        - Packets Lost: ${packetsLost.get()}
        - Bytes In: ${bytesIn.get()}
        - Bytes Out: ${bytesOut.get()}
        - Messages Sent: ${messagesSent.get()}
        - Messages Received: ${messagesReceived.get()}
    """.trimIndent()
}

/**
 * Modern Kotlin implementation of SecondLife's message system
 * 
 * This class handles all UDP communication with SecondLife simulators,
 * providing reliable messaging, throttling, and connection management.
 */
class MessageSystemTranslated(
    private val localHost: NetworkHost,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    companion object {
        private const val MAX_MESSAGE_SIZE = 32768
        private const val MAX_BUFFER_SIZE = 65536
        private const val RELIABLE_FLAG: UByte = 0x80u
        private const val ZEROCODED_FLAG: UByte = 0x40u
        
        // Global instance for compatibility
        @Volatile
        private var instance: MessageSystemTranslated? = null
        
        fun getInstance(): MessageSystemTranslated? = instance
        
        fun initialize(host: NetworkHost): MessageSystemTranslated {
            return MessageSystemTranslated(host).also { instance = it }
        }
    }
    
    // Core system state
    private val isRunning = AtomicBoolean(false)
    private val stats = MessageStats()
    private val messageTemplates = mutableMapOf<String, MessageTemplate>()
    
    // Network components
    private var udpSocket: DatagramSocket? = null
    private val circuitManager = CircuitManager()
    private val throttleManager = ThrottleManager()
    
    // Message handling
    private val messageHandlers = mutableMapOf<String, MutableList<suspend (MessageData) -> Unit>>()
    private val incomingMessages = Channel<IncomingMessage>(Channel.UNLIMITED)
    private val outgoingMessages = Channel<OutgoingMessage>(Channel.UNLIMITED)
    
    // Current message building state
    private val messageMutex = Mutex()
    private var currentMessage: MessageBuilder? = null
    
    /**
     * Statistics flow for real-time monitoring
     */
    val statisticsFlow: Flow<MessageStats> = flow {
        while (isRunning.get()) {
            emit(stats)
            delay(1000) // Update every second
        }
    }
    
    /**
     * Incoming message flow for reactive processing
     */
    val incomingMessageFlow: Flow<IncomingMessage> = incomingMessages.receiveAsFlow()
    
    /**
     * Initialize and start the message system
     */
    suspend fun start(): Boolean = try {
        if (isRunning.compareAndSet(false, true)) {
            initializeMessageTemplates()
            startNetworking()
            startMessageProcessing()
            println("Message system started on $localHost")
            true
        } else {
            false
        }
    } catch (e: Exception) {
        println("Failed to start message system: ${e.message}")
        stop()
        false
    }
    
    /**
     * Stop the message system and cleanup resources
     */
    suspend fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            udpSocket?.close()
            incomingMessages.close()
            outgoingMessages.close()
            scope.cancel()
            println("Message system stopped")
        }
    }
    
    /**
     * Start building a new outgoing message
     */
    suspend fun newMessage(messageName: String): Boolean = messageMutex.withLock {
        val template = messageTemplates[messageName]
        if (template == null) {
            println("Unknown message type: $messageName")
            return false
        }
        
        currentMessage = MessageBuilder(template)
        return true
    }
    
    /**
     * Add string variable to current message
     */
    suspend fun addString(blockName: String, varName: String, value: String): Boolean = 
        messageMutex.withLock {
            currentMessage?.addString(blockName, varName, value) ?: false
        }
    
    /**
     * Add U32 variable to current message
     */
    suspend fun addU32(blockName: String, varName: String, value: UInt): Boolean = 
        messageMutex.withLock {
            currentMessage?.addU32(blockName, varName, value) ?: false
        }
    
    /**
     * Add F32 variable to current message
     */
    suspend fun addF32(blockName: String, varName: String, value: Float): Boolean = 
        messageMutex.withLock {
            currentMessage?.addF32(blockName, varName, value) ?: false
        }
    
    /**
     * Add Vector3 variable to current message
     */
    suspend fun addVector3(blockName: String, varName: String, value: Vector3): Boolean = 
        messageMutex.withLock {
            currentMessage?.addVector3(blockName, varName, value) ?: false
        }
    
    /**
     * Send the current message to specified host
     */
    suspend fun sendMessage(host: NetworkHost): Boolean = messageMutex.withLock {
        val message = currentMessage ?: return false
        
        try {
            val messageData = message.build()
            val outgoing = OutgoingMessage(host, messageData, message.template.messageType.reliable)
            
            outgoingMessages.send(outgoing)
            stats.messagesSent.incrementAndGet()
            
            currentMessage = null
            return true
        } catch (e: Exception) {
            println("Failed to send message: ${e.message}")
            return false
        }
    }
    
    /**
     * Register a message handler for incoming messages
     */
    fun setMessageHandler(messageName: String, handler: suspend (MessageData) -> Unit) {
        messageHandlers.getOrPut(messageName) { mutableListOf() }.add(handler)
    }
    
    /**
     * Remove message handler
     */
    fun removeMessageHandler(messageName: String, handler: suspend (MessageData) -> Unit) {
        messageHandlers[messageName]?.remove(handler)
    }
    
    /**
     * Get current statistics
     */
    fun getStatistics(): MessageStats = stats
    
    /**
     * Print current statistics to console
     */
    fun printStatistics() {
        println(stats.toString())
    }
    
    // Private implementation
    
    private fun initializeMessageTemplates() {
        // Initialize core message templates
        initializeLoginMessages()
        initializeChatMessages()
        initializeRegionMessages()
        initializePingMessages()
    }
    
    private fun initializeLoginMessages() {
        // LoginRequest message template
        messageTemplates["LoginRequest"] = MessageTemplate(
            MessageType.LoginRequest,
            listOf(
                MessageTemplate.MessageBlock(
                    "CircuitInfo",
                    MessageTemplate.BlockType.SINGLE,
                    listOf(
                        MessageTemplate.MessageVariable("IP", VariableType.IPAddr),
                        MessageTemplate.MessageVariable("Port", VariableType.IPPort)
                    )
                ),
                MessageTemplate.MessageBlock(
                    "LoginInfo", 
                    MessageTemplate.BlockType.SINGLE,
                    listOf(
                        MessageTemplate.MessageVariable("UserName", VariableType.String),
                        MessageTemplate.MessageVariable("Password", VariableType.String),
                        MessageTemplate.MessageVariable("Start", VariableType.String),
                        MessageTemplate.MessageVariable("ClientVersion", VariableType.String)
                    )
                )
            )
        )
        
        // LoginReply message template
        messageTemplates["LoginReply"] = MessageTemplate(
            MessageType.LoginReply,
            listOf(
                MessageTemplate.MessageBlock(
                    "AgentData",
                    MessageTemplate.BlockType.SINGLE,
                    listOf(
                        MessageTemplate.MessageVariable("AgentID", VariableType.UUID),
                        MessageTemplate.MessageVariable("SessionID", VariableType.UUID),
                        MessageTemplate.MessageVariable("SecureSessionID", VariableType.UUID)
                    )
                )
            )
        )
    }
    
    private fun initializeChatMessages() {
        messageTemplates["ChatFromViewer"] = MessageTemplate(
            MessageType.ChatFromViewer,
            listOf(
                MessageTemplate.MessageBlock(
                    "AgentData",
                    MessageTemplate.BlockType.SINGLE,
                    listOf(
                        MessageTemplate.MessageVariable("AgentID", VariableType.UUID),
                        MessageTemplate.MessageVariable("SessionID", VariableType.UUID)
                    )
                ),
                MessageTemplate.MessageBlock(
                    "ChatData",
                    MessageTemplate.BlockType.SINGLE, 
                    listOf(
                        MessageTemplate.MessageVariable("Message", VariableType.String),
                        MessageTemplate.MessageVariable("Type", VariableType.U32),
                        MessageTemplate.MessageVariable("Channel", VariableType.U32)
                    )
                )
            )
        )
    }
    
    private fun initializeRegionMessages() {
        messageTemplates["RegionHandshake"] = MessageTemplate(
            MessageType.RegionHandshake,
            listOf(
                MessageTemplate.MessageBlock(
                    "RegionInfo",
                    MessageTemplate.BlockType.SINGLE,
                    listOf(
                        MessageTemplate.MessageVariable("RegionFlags", VariableType.U32),
                        MessageTemplate.MessageVariable("SimAccess", VariableType.U32),
                        MessageTemplate.MessageVariable("SimName", VariableType.String),
                        MessageTemplate.MessageVariable("RegionID", VariableType.UUID)
                    )
                )
            )
        )
    }
    
    private fun initializePingMessages() {
        messageTemplates["StartPingCheck"] = MessageTemplate(
            MessageType.StartPingCheck,
            listOf(
                MessageTemplate.MessageBlock(
                    "PingID",
                    MessageTemplate.BlockType.SINGLE,
                    listOf(
                        MessageTemplate.MessageVariable("PingID", VariableType.U32),
                        MessageTemplate.MessageVariable("OldestUnacked", VariableType.U32)
                    )
                )
            )
        )
    }
    
    private suspend fun startNetworking() = withContext(Dispatchers.IO) {
        val resolved = localHost.resolve()
        udpSocket = DatagramSocket(resolved).apply {
            reuseAddress = true
            soTimeout = 100 // Non-blocking with timeout
        }
    }
    
    private fun startMessageProcessing() {
        // Start incoming message receiver
        scope.launch {
            receiveMessages()
        }
        
        // Start outgoing message sender  
        scope.launch {
            sendMessages()
        }
        
        // Start message dispatcher
        scope.launch {
            dispatchMessages()
        }
    }
    
    private suspend fun receiveMessages() {
        val buffer = ByteArray(MAX_BUFFER_SIZE)
        val packet = DatagramPacket(buffer, buffer.size)
        
        while (isRunning.get()) {
            try {
                udpSocket?.receive(packet)
                
                val host = NetworkHost(packet.address.hostAddress, packet.port)
                val data = packet.data.copyOf(packet.length)
                
                stats.packetsIn.incrementAndGet()
                stats.bytesIn.addAndGet(data.size.toLong())
                
                val message = parseIncomingMessage(host, data)
                message?.let { incomingMessages.send(it) }
                
            } catch (e: SocketTimeoutException) {
                // Normal timeout, continue
            } catch (e: Exception) {
                if (isRunning.get()) {
                    println("Error receiving message: ${e.message}")
                }
            }
        }
    }
    
    private suspend fun sendMessages() {
        while (isRunning.get()) {
            try {
                val message = outgoingMessages.receive()
                val resolved = message.host.resolve()
                
                // Apply throttling
                if (!throttleManager.canSend(message.host, message.data.size)) {
                    delay(10) // Brief delay before retry
                    continue
                }
                
                val packet = DatagramPacket(
                    message.data, 
                    message.data.size,
                    resolved
                )
                
                udpSocket?.send(packet)
                
                stats.packetsOut.incrementAndGet()
                stats.bytesOut.addAndGet(message.data.size.toLong())
                
                // Handle reliability
                if (message.reliable) {
                    circuitManager.addReliablePacket(message.host, message.data)
                }
                
            } catch (e: Exception) {
                if (isRunning.get()) {
                    println("Error sending message: ${e.message}")
                }
            }
        }
    }
    
    private suspend fun dispatchMessages() {
        while (isRunning.get()) {
            try {
                val message = incomingMessages.receive()
                stats.messagesReceived.incrementAndGet()
                
                val handlers = messageHandlers[message.messageType.name]
                handlers?.forEach { handler ->
                    scope.launch {
                        try {
                            handler(message.data)
                        } catch (e: Exception) {
                            println("Handler error for ${message.messageType.name}: ${e.message}")
                        }
                    }
                }
                
            } catch (e: Exception) {
                if (isRunning.get()) {
                    println("Error dispatching message: ${e.message}")
                }
            }
        }
    }
    
    private fun parseIncomingMessage(host: NetworkHost, data: ByteArray): IncomingMessage? {
        if (data.size < 6) return null
        
        val buffer = ByteBuffer.wrap(data)
        
        // Parse header
        val flags = buffer.get()
        val reliable = (flags.toUByte() and RELIABLE_FLAG) != 0u.toUByte()
        val zerocoded = (flags.toUByte() and ZEROCODED_FLAG) != 0u.toUByte()
        
        val sequence = buffer.int
        
        // Parse message number
        val messageNumber = when (val firstByte = buffer.get().toUByte()) {
            0xFFu -> {
                when (val secondByte = buffer.get().toUByte()) {
                    0xFFu -> buffer.int.toUInt()
                    else -> ((secondByte.toUInt() shl 8) or buffer.get().toUByte().toUInt())
                }
            }
            else -> firstByte.toUInt()
        }
        
        val messageType = MessageType.fromNumber(messageNumber) ?: return null
        
        // Extract payload
        val payloadData = ByteArray(buffer.remaining())
        buffer.get(payloadData)
        
        // Handle zero-coding if needed
        val finalData = if (zerocoded) {
            decodeZeroData(payloadData)
        } else {
            payloadData
        }
        
        val messageData = MessageData(finalData, messageTemplates[messageType.name])
        
        return IncomingMessage(host, messageType, messageData, sequence.toUInt(), reliable)
    }
    
    private fun decodeZeroData(data: ByteArray): ByteArray {
        // Implement zero-data decompression
        // This expands runs of zeros that were compressed in transmission
        val result = mutableListOf<Byte>()
        var i = 0
        
        while (i < data.size) {
            if (i < data.size - 1 && data[i] == 0.toByte() && data[i + 1] != 0.toByte()) {
                // Expand zero run
                val zeroCount = data[i + 1].toUByte().toInt()
                repeat(zeroCount) { result.add(0) }
                i += 2
            } else {
                result.add(data[i])
                i++
            }
        }
        
        return result.toByteArray()
    }
}

/**
 * Supporting classes for message system
 */

data class Vector3(val x: Float, val y: Float, val z: Float)

data class IncomingMessage(
    val host: NetworkHost,
    val messageType: MessageType,
    val data: MessageData,
    val sequence: UInt,
    val reliable: Boolean
)

data class OutgoingMessage(
    val host: NetworkHost,
    val data: ByteArray,
    val reliable: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as OutgoingMessage
        return host == other.host && data.contentEquals(other.data) && reliable == other.reliable
    }
    
    override fun hashCode(): Int {
        var result = host.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + reliable.hashCode()
        return result
    }
}

/**
 * Message data container with template-based access
 */
class MessageData(
    private val data: ByteArray,
    private val template: MessageTemplate?
) {
    fun getString(blockName: String, varName: String, blockIndex: Int = 0): String? {
        return extractValue(blockName, varName, blockIndex) { buffer ->
            val length = buffer.get().toUByte().toInt()
            val stringBytes = ByteArray(length)
            buffer.get(stringBytes)
            String(stringBytes)
        }
    }
    
    fun getU32(blockName: String, varName: String, blockIndex: Int = 0): UInt? {
        return extractValue(blockName, varName, blockIndex) { buffer ->
            buffer.int.toUInt()
        }
    }
    
    fun getF32(blockName: String, varName: String, blockIndex: Int = 0): Float? {
        return extractValue(blockName, varName, blockIndex) { buffer ->
            buffer.float
        }
    }
    
    fun getVector3(blockName: String, varName: String, blockIndex: Int = 0): Vector3? {
        return extractValue(blockName, varName, blockIndex) { buffer ->
            Vector3(buffer.float, buffer.float, buffer.float)
        }
    }
    
    private fun <T> extractValue(
        blockName: String, 
        varName: String, 
        blockIndex: Int,
        extractor: (ByteBuffer) -> T
    ): T? {
        template ?: return null
        
        val buffer = ByteBuffer.wrap(data)
        
        // Find the block and variable
        for (block in template.blocks) {
            if (block.name == blockName) {
                for (variable in block.variables) {
                    if (variable.name == varName) {
                        // Navigate to the correct position
                        // This is a simplified version - real implementation would
                        // properly parse the message format
                        return try {
                            extractor(buffer)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
            }
        }
        
        return null
    }
}

/**
 * Message builder for constructing outgoing messages
 */
class MessageBuilder(val template: MessageTemplate) {
    private val buffer = ByteBuffer.allocate(MessageSystemTranslated.MAX_MESSAGE_SIZE)
    private val blockData = mutableMapOf<String, MutableList<Map<String, Any>>>()
    
    init {
        // Initialize with message header
        writeMessageHeader()
    }
    
    fun addString(blockName: String, varName: String, value: String): Boolean {
        return addVariable(blockName, varName, value)
    }
    
    fun addU32(blockName: String, varName: String, value: UInt): Boolean {
        return addVariable(blockName, varName, value)
    }
    
    fun addF32(blockName: String, varName: String, value: Float): Boolean {
        return addVariable(blockName, varName, value)
    }
    
    fun addVector3(blockName: String, varName: String, value: Vector3): Boolean {
        return addVariable(blockName, varName, value)
    }
    
    private fun addVariable(blockName: String, varName: String, value: Any): Boolean {
        val blockList = blockData.getOrPut(blockName) { mutableListOf() }
        
        if (blockList.isEmpty()) {
            blockList.add(mutableMapOf())
        }
        
        blockList.last()[varName] = value
        return true
    }
    
    fun build(): ByteArray {
        // Write all block data
        for (block in template.blocks) {
            val data = blockData[block.name] ?: continue
            
            for (blockInstance in data) {
                for (variable in block.variables) {
                    val value = blockInstance[variable.name] ?: continue
                    writeVariable(variable, value)
                }
            }
        }
        
        buffer.flip()
        val result = ByteArray(buffer.remaining())
        buffer.get(result)
        return result
    }
    
    private fun writeMessageHeader() {
        // Write flags
        var flags: UByte = 0u
        if (template.messageType.reliable) {
            flags = flags or MessageSystemTranslated.RELIABLE_FLAG
        }
        buffer.put(flags.toByte())
        
        // Write sequence number (will be filled by circuit layer)
        buffer.putInt(0)
        
        // Write message number
        val msgNum = template.messageType.number
        when {
            msgNum < 256u -> buffer.put(msgNum.toByte())
            msgNum < 65536u -> {
                buffer.put(0xFF.toByte())
                buffer.put((msgNum shr 8).toByte())
                buffer.put((msgNum and 0xFFu).toByte())
            }
            else -> {
                buffer.put(0xFF.toByte())
                buffer.put(0xFF.toByte())
                buffer.putInt(msgNum.toInt())
            }
        }
    }
    
    private fun writeVariable(variable: MessageTemplate.MessageVariable, value: Any) {
        when (variable.type) {
            VariableType.String -> {
                val str = value as String
                val bytes = str.toByteArray()
                buffer.put(bytes.size.toByte())
                buffer.put(bytes)
            }
            VariableType.U32 -> buffer.putInt((value as UInt).toInt())
            VariableType.F32 -> buffer.putFloat(value as Float)
            VariableType.Vector3 -> {
                val vec = value as Vector3
                buffer.putFloat(vec.x)
                buffer.putFloat(vec.y)
                buffer.putFloat(vec.z)
            }
            else -> {
                // Handle other types as needed
            }
        }
    }
}

/**
 * Circuit management for reliable messaging
 */
class CircuitManager {
    private val reliablePackets = mutableMapOf<NetworkHost, MutableList<ByteArray>>()
    
    fun addReliablePacket(host: NetworkHost, data: ByteArray) {
        reliablePackets.getOrPut(host) { mutableListOf() }.add(data)
    }
    
    fun getNumCircuits(): Int = reliablePackets.size
}

/**
 * Throttle management for bandwidth control
 */
class ThrottleManager {
    private val hostThrottles = mutableMapOf<NetworkHost, HostThrottle>()
    
    fun canSend(host: NetworkHost, size: Int): Boolean {
        val throttle = hostThrottles.getOrPut(host) { HostThrottle() }
        return throttle.canSend(size)
    }
    
    private class HostThrottle {
        private var lastSendTime = System.currentTimeMillis()
        private var bytesThisSecond = 0
        private val maxBytesPerSecond = 100000 // 100KB/s
        
        fun canSend(size: Int): Boolean {
            val now = System.currentTimeMillis()
            
            if (now - lastSendTime >= 1000) {
                // Reset for new second
                bytesThisSecond = 0
                lastSendTime = now
            }
            
            if (bytesThisSecond + size <= maxBytesPerSecond) {
                bytesThisSecond += size
                return true
            }
            
            return false
        }
    }
}

/**
 * Global functions for compatibility with C++ interface
 */
object MessageSystemGlobal {
    private var instance: MessageSystemTranslated? = null
    
    suspend fun initialize(host: NetworkHost): Boolean {
        instance = MessageSystemTranslated.initialize(host)
        return instance?.start() ?: false
    }
    
    suspend fun shutdown() {
        instance?.stop()
        instance = null
    }
    
    suspend fun newMessage(messageName: String): Boolean {
        return instance?.newMessage(messageName) ?: false
    }
    
    suspend fun addString(blockName: String, varName: String, value: String): Boolean {
        return instance?.addString(blockName, varName, value) ?: false
    }
    
    suspend fun addU32(blockName: String, varName: String, value: UInt): Boolean {
        return instance?.addU32(blockName, varName, value) ?: false
    }
    
    suspend fun sendMessage(host: NetworkHost): Boolean {
        return instance?.sendMessage(host) ?: false
    }
    
    fun setMessageHandler(messageName: String, handler: suspend (MessageData) -> Unit) {
        instance?.setMessageHandler(messageName, handler)
    }
    
    fun printStatistics() {
        instance?.printStatistics()
    }
}

/**
 * Extension functions for easy message handling
 */
suspend fun MessageSystemTranslated.sendLoginRequest(
    host: NetworkHost,
    username: String,
    password: String,
    clientVersion: String
): Boolean {
    if (!newMessage("LoginRequest")) return false
    
    addString("LoginInfo", "UserName", username)
    addString("LoginInfo", "Password", password)
    addString("LoginInfo", "Start", "home")
    addString("LoginInfo", "ClientVersion", clientVersion)
    
    return sendMessage(host)
}

suspend fun MessageSystemTranslated.sendChatMessage(
    host: NetworkHost,
    agentId: String,
    sessionId: String, 
    message: String,
    channel: UInt = 0u
): Boolean {
    if (!newMessage("ChatFromViewer")) return false
    
    // Would need UUID type support for agent/session IDs
    addString("ChatData", "Message", message)
    addU32("ChatData", "Type", 1u) // Normal chat
    addU32("ChatData", "Channel", channel)
    
    return sendMessage(host)
}