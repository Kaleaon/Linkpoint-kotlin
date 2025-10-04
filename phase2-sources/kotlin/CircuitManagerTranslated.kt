/**
 * CircuitManagerTranslated.kt
 *
 * Modern Kotlin translation of SecondLife's circuit management system.
 * This provides the reliability layer on top of UDP messaging, handling 
 * packet acknowledgments, retransmissions, timeouts, and connection state.
 *
 * Original: https://github.com/secondlife/viewer/blob/main/indra/llmessage/llcircuit.cpp
 *
 * Key improvements in Kotlin version:
 * - Coroutine-based timeouts and retries instead of polling
 * - Type-safe circuit state management with sealed classes
 * - Flow-based statistics and monitoring streams
 * - Automatic resource cleanup with coroutine scopes
 * - Enhanced error handling and circuit recovery
 * - Real-time packet loss detection and adaptation
 */

package com.secondlife.viewer.network

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.*
import kotlinx.serialization.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*
import kotlin.time.*

/**
 * Circuit state management with type safety
 */
sealed class CircuitState {
    object Initializing : CircuitState()
    object Active : CircuitState()
    object Degraded : CircuitState()
    object Blocked : CircuitState()
    object Timeout : CircuitState()
    object Closed : CircuitState()
    
    val isActive: Boolean get() = this is Active || this is Degraded
    val canSend: Boolean get() = this is Active || this is Degraded
}

/**
 * Packet reliability levels
 */
enum class ReliabilityLevel {
    UNRELIABLE,     // Fire and forget
    RELIABLE,       // Acknowledgment required
    CRITICAL        // Multiple retries with exponential backoff
}

/**
 * Packet buffer for reliable transmission
 */
@Serializable
data class PacketBuffer(
    val sequenceNumber: UInt,
    val data: ByteArray,
    val reliability: ReliabilityLevel,
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val maxRetries: Int = 3
) {
    fun withRetry(): PacketBuffer = copy(
        retryCount = retryCount + 1,
        timestamp = System.currentTimeMillis()
    )
    
    val isExpired: Boolean get() = retryCount >= maxRetries
    val canRetry: Boolean get() = retryCount < maxRetries
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PacketBuffer
        return sequenceNumber == other.sequenceNumber && data.contentEquals(other.data)
    }
    
    override fun hashCode(): Int {
        var result = sequenceNumber.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * Circuit statistics with atomic updates
 */
data class CircuitStatistics(
    val packetsOut: AtomicLong = AtomicLong(0),
    val packetsIn: AtomicLong = AtomicLong(0),
    val packetsLost: AtomicLong = AtomicLong(0),
    val bytesOut: AtomicLong = AtomicLong(0),
    val bytesIn: AtomicLong = AtomicLong(0),
    val avgPingMs: AtomicReference<Double> = AtomicReference(0.0),
    val currentPingMs: AtomicReference<Double> = AtomicReference(0.0),
    val lastReceiveTime: AtomicLong = AtomicLong(System.currentTimeMillis()),
    val lastSendTime: AtomicLong = AtomicLong(System.currentTimeMillis())
) {
    val packetLossPercentage: Double
        get() {
            val totalOut = packetsOut.get()
            val lost = packetsLost.get()
            return if (totalOut > 0) (lost.toDouble() / totalOut.toDouble()) * 100.0 else 0.0
        }
    
    val isHealthy: Boolean
        get() = packetLossPercentage < 5.0 && avgPingMs.get() < 1000.0
    
    fun updatePing(pingMs: Double) {
        currentPingMs.set(pingMs)
        val current = avgPingMs.get()
        val updated = if (current == 0.0) pingMs else (current * 0.9 + pingMs * 0.1)
        avgPingMs.set(updated)
    }
    
    fun reset() {
        packetsOut.set(0)
        packetsIn.set(0)
        packetsLost.set(0)
        bytesOut.set(0)
        bytesIn.set(0)
        avgPingMs.set(0.0)
        currentPingMs.set(0.0)
        lastReceiveTime.set(System.currentTimeMillis())
        lastSendTime.set(System.currentTimeMillis())
    }
    
    override fun toString(): String = """
        Circuit Statistics:
        - Packets Out: ${packetsOut.get()}
        - Packets In: ${packetsIn.get()}
        - Packets Lost: ${packetsLost.get()}
        - Packet Loss: ${"%.2f".format(packetLossPercentage)}%
        - Bytes Out: ${bytesOut.get()}
        - Bytes In: ${bytesIn.get()}
        - Current Ping: ${"%.2f".format(currentPingMs.get())}ms
        - Average Ping: ${"%.2f".format(avgPingMs.get())}ms
        - Health: ${if (isHealthy) "Good" else "Poor"}
    """.trimIndent()
}

/**
 * Individual circuit data for a specific network host
 */
class CircuitData(
    val host: NetworkHost,
    private val scope: CoroutineScope
) {
    // Circuit state
    private val _state = MutableStateFlow<CircuitState>(CircuitState.Initializing)
    val state: StateFlow<CircuitState> = _state.asStateFlow()
    
    // Statistics
    val statistics = CircuitStatistics()
    
    // Sequence management
    private val nextOutgoingSequence = AtomicReference<UInt>(1u)
    private val nextExpectedIncoming = AtomicReference<UInt>(1u)
    private val oldestUnackedSequence = AtomicReference<UInt>(0u)
    
    // Reliable packet management
    private val unackedPackets = ConcurrentHashMap<UInt, PacketBuffer>()
    private val retryQueue = Channel<PacketBuffer>(Channel.UNLIMITED)
    private val ackChannel = Channel<UInt>(Channel.UNLIMITED)
    
    // Timing and throttling
    private var throttleManager: ThrottleManager? = null
    
    // Monitoring flows
    val stateFlow: StateFlow<CircuitState> = _state.asStateFlow()
    val healthFlow: Flow<Boolean> = stateFlow.map { it.isActive && statistics.isHealthy }
    
    init {
        startCircuitMonitoring()
        startRetryProcessor()
        startAckProcessor()
    }
    
    /**
     * Generate next outgoing sequence number
     */
    fun nextPacketSequence(): UInt {
        statistics.lastSendTime.set(System.currentTimeMillis())
        statistics.packetsOut.incrementAndGet()
        return nextOutgoingSequence.getAndUpdate { it + 1u }
    }
    
    /**
     * Process incoming packet with sequence checking
     */
    fun processIncomingPacket(sequenceNumber: UInt, dataSize: Int, isResend: Boolean = false) {
        statistics.lastReceiveTime.set(System.currentTimeMillis())
        statistics.packetsIn.incrementAndGet()
        statistics.bytesIn.addAndGet(dataSize.toLong())
        
        val expected = nextExpectedIncoming.get()
        
        when {
            sequenceNumber == expected -> {
                // Perfect sequence
                nextExpectedIncoming.set(expected + 1u)
                updateCircuitHealth(true)
            }
            sequenceNumber > expected -> {
                // Gap detected - estimate lost packets
                val gapSize = (sequenceNumber - expected).toInt()
                statistics.packetsLost.addAndGet(gapSize.toLong())
                nextExpectedIncoming.set(sequenceNumber + 1u)
                updateCircuitHealth(false)
            }
            else -> {
                // Out of order or duplicate - handled gracefully
                if (!isResend) {
                    println("Out of order packet from ${host}: expected $expected, got $sequenceNumber")
                }
            }
        }
    }
    
    /**
     * Add reliable packet for transmission tracking
     */
    suspend fun addReliablePacket(packet: PacketBuffer) {
        unackedPackets[packet.sequenceNumber] = packet
        
        // Update oldest unacked if this is the first
        if (unackedPackets.size == 1) {
            oldestUnackedSequence.set(packet.sequenceNumber)
        }
        
        // Schedule timeout check
        scope.launch {
            delay(getTimeoutDelay(packet.reliability))
            checkPacketTimeout(packet.sequenceNumber)
        }
    }
    
    /**
     * Process acknowledgment for reliable packet
     */
    suspend fun acknowledgePacket(sequenceNumber: UInt) {
        ackChannel.send(sequenceNumber)
    }
    
    /**
     * Get retry delay based on reliability level and retry count
     */
    private fun getRetryDelay(packet: PacketBuffer): Duration {
        val baseDelay = when (packet.reliability) {
            ReliabilityLevel.UNRELIABLE -> return Duration.ZERO
            ReliabilityLevel.RELIABLE -> 1.seconds
            ReliabilityLevel.CRITICAL -> 500.milliseconds
        }
        
        // Exponential backoff
        val multiplier = kotlin.math.pow(2.0, packet.retryCount.toDouble()).toInt()
        return baseDelay * multiplier
    }
    
    /**
     * Get timeout delay based on reliability level
     */
    private fun getTimeoutDelay(reliability: ReliabilityLevel): Duration {
        return when (reliability) {
            ReliabilityLevel.UNRELIABLE -> Duration.ZERO
            ReliabilityLevel.RELIABLE -> 5.seconds
            ReliabilityLevel.CRITICAL -> 3.seconds
        }
    }
    
    /**
     * Check for packet timeout and handle retries
     */
    private suspend fun checkPacketTimeout(sequenceNumber: UInt) {
        val packet = unackedPackets[sequenceNumber] ?: return
        
        if (packet.canRetry) {
            retryQueue.send(packet.withRetry())
        } else {
            // Packet expired - mark as lost
            unackedPackets.remove(sequenceNumber)
            statistics.packetsLost.incrementAndGet()
            updateCircuitHealth(false)
            
            println("Packet $sequenceNumber to $host expired after ${packet.maxRetries} retries")
        }
    }
    
    /**
     * Update circuit health based on recent activity
     */
    private fun updateCircuitHealth(positive: Boolean) {
        val currentState = _state.value
        
        val newState = when {
            positive && currentState !is CircuitState.Active -> CircuitState.Active
            !positive && statistics.packetLossPercentage > 10.0 -> CircuitState.Degraded
            !positive && statistics.packetLossPercentage > 25.0 -> CircuitState.Blocked
            else -> currentState
        }
        
        if (newState != currentState) {
            _state.value = newState
            println("Circuit to $host state changed: $currentState -> $newState")
        }
    }
    
    /**
     * Start circuit monitoring coroutine
     */
    private fun startCircuitMonitoring() {
        scope.launch {
            while (true) {
                delay(10.seconds)
                
                val now = System.currentTimeMillis()
                val lastReceive = statistics.lastReceiveTime.get()
                val timeSinceLastReceive = now - lastReceive
                
                when {
                    timeSinceLastReceive > 60_000 -> {
                        // 1 minute timeout
                        _state.value = CircuitState.Timeout
                        println("Circuit to $host timed out (${timeSinceLastReceive}ms since last receive)")
                    }
                    timeSinceLastReceive > 30_000 -> {
                        // 30 seconds - degraded
                        if (_state.value.isActive) {
                            _state.value = CircuitState.Degraded
                            println("Circuit to $host degraded (${timeSinceLastReceive}ms since last receive)")
                        }
                    }
                    else -> {
                        // Circuit is active
                        if (_state.value != CircuitState.Active && statistics.isHealthy) {
                            _state.value = CircuitState.Active
                            println("Circuit to $host recovered")
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Start retry processor coroutine
     */
    private fun startRetryProcessor() {
        scope.launch {
            for (packet in retryQueue) {
                if (packet.canRetry) {
                    delay(getRetryDelay(packet))
                    
                    // Update packet in unacked list
                    unackedPackets[packet.sequenceNumber] = packet
                    
                    println("Retrying packet ${packet.sequenceNumber} to $host (attempt ${packet.retryCount + 1})")
                    // Actual retry would be handled by message system
                    
                    // Schedule next timeout check
                    scope.launch {
                        delay(getTimeoutDelay(packet.reliability))
                        checkPacketTimeout(packet.sequenceNumber)
                    }
                }
            }
        }
    }
    
    /**
     * Start acknowledgment processor coroutine
     */
    private fun startAckProcessor() {
        scope.launch {
            for (sequenceNumber in ackChannel) {
                val packet = unackedPackets.remove(sequenceNumber)
                if (packet != null) {
                    // Calculate ping time
                    val pingMs = System.currentTimeMillis() - packet.timestamp
                    statistics.updatePing(pingMs.toDouble())
                    
                    // Update oldest unacked
                    if (unackedPackets.isNotEmpty()) {
                        val oldest = unackedPackets.keys.minOrNull()
                        oldest?.let { oldestUnackedSequence.set(it) }
                    }
                    
                    updateCircuitHealth(true)
                }
            }
        }
    }
    
    /**
     * Get current unacknowledged packet count
     */
    fun getUnackedCount(): Int = unackedPackets.size
    
    /**
     * Get oldest unacknowledged sequence number
     */
    fun getOldestUnacked(): UInt = oldestUnackedSequence.get()
    
    /**
     * Close the circuit and cleanup resources
     */
    suspend fun close() {
        _state.value = CircuitState.Closed
        retryQueue.close()
        ackChannel.close()
        unackedPackets.clear()
    }
}

/**
 * Modern Kotlin circuit manager for network reliability
 */
class CircuitManagerTranslated(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    companion object {
        private const val MAX_CIRCUITS = 256
        private const val CLEANUP_INTERVAL_MS = 30_000L
    }
    
    // Circuit management
    private val circuits = ConcurrentHashMap<NetworkHost, CircuitData>()
    private val circuitMutex = Mutex()
    
    // Configuration
    private var allowTimeout = true
    private var maxCircuits = MAX_CIRCUITS
    
    // Monitoring flows
    val circuitCountFlow: Flow<Int> = flow {
        while (true) {
            emit(circuits.size)
            delay(1.seconds)
        }
    }.flowOn(Dispatchers.IO)
    
    val globalStatisticsFlow: Flow<GlobalCircuitStats> = flow {
        while (true) {
            emit(calculateGlobalStats())
            delay(5.seconds)
        }
    }.flowOn(Dispatchers.IO)
    
    init {
        startCircuitCleanup()
    }
    
    /**
     * Find or create circuit for host
     */
    suspend fun getOrCreateCircuit(host: NetworkHost): CircuitData? = circuitMutex.withLock {
        circuits[host] ?: run {
            if (circuits.size >= maxCircuits) {
                println("Maximum circuits ($maxCircuits) reached, cannot create circuit for $host")
                return null
            }
            
            val circuit = CircuitData(host, scope)
            circuits[host] = circuit
            
            println("Created circuit for $host")
            circuit
        }
    }
    
    /**
     * Find existing circuit for host
     */
    fun findCircuit(host: NetworkHost): CircuitData? = circuits[host]
    
    /**
     * Remove circuit for host
     */
    suspend fun removeCircuit(host: NetworkHost) = circuitMutex.withLock {
        circuits.remove(host)?.let { circuit ->
            circuit.close()
            println("Removed circuit for $host")
        }
    }
    
    /**
     * Generate next packet sequence for host
     */
    suspend fun nextPacketSequence(host: NetworkHost): UInt? {
        return getOrCreateCircuit(host)?.nextPacketSequence()
    }
    
    /**
     * Process incoming packet
     */
    suspend fun processIncomingPacket(
        host: NetworkHost, 
        sequenceNumber: UInt, 
        dataSize: Int,
        isResend: Boolean = false
    ) {
        getOrCreateCircuit(host)?.processIncomingPacket(sequenceNumber, dataSize, isResend)
    }
    
    /**
     * Add reliable packet for tracking
     */
    suspend fun addReliablePacket(host: NetworkHost, packet: PacketBuffer) {
        getOrCreateCircuit(host)?.addReliablePacket(packet)
    }
    
    /**
     * Acknowledge reliable packet
     */
    suspend fun acknowledgePacket(host: NetworkHost, sequenceNumber: UInt) {
        findCircuit(host)?.acknowledgePacket(sequenceNumber)
    }
    
    /**
     * Get circuit statistics for host
     */
    fun getCircuitStatistics(host: NetworkHost): CircuitStatistics? {
        return findCircuit(host)?.statistics
    }
    
    /**
     * Get all active circuits
     */
    fun getActiveCircuits(): List<Pair<NetworkHost, CircuitData>> {
        return circuits.filter { it.value.state.value.isActive }.toList()
    }
    
    /**
     * Calculate global statistics across all circuits
     */
    private fun calculateGlobalStats(): GlobalCircuitStats {
        val allCircuits = circuits.values.toList()
        
        return GlobalCircuitStats(
            totalCircuits = allCircuits.size,
            activeCircuits = allCircuits.count { it.state.value.isActive },
            degradedCircuits = allCircuits.count { it.state.value is CircuitState.Degraded },
            blockedCircuits = allCircuits.count { it.state.value is CircuitState.Blocked },
            timeoutCircuits = allCircuits.count { it.state.value is CircuitState.Timeout },
            totalPacketsOut = allCircuits.sumOf { it.statistics.packetsOut.get() },
            totalPacketsIn = allCircuits.sumOf { it.statistics.packetsIn.get() },
            totalPacketsLost = allCircuits.sumOf { it.statistics.packetsLost.get() },
            totalBytesOut = allCircuits.sumOf { it.statistics.bytesOut.get() },
            totalBytesIn = allCircuits.sumOf { it.statistics.bytesIn.get() },
            averagePing = allCircuits.mapNotNull { 
                val ping = it.statistics.avgPingMs.get()
                if (ping > 0.0) ping else null
            }.average().takeIf { !it.isNaN() } ?: 0.0,
            totalUnackedPackets = allCircuits.sumOf { it.getUnackedCount() }
        )
    }
    
    /**
     * Start circuit cleanup coroutine
     */
    private fun startCircuitCleanup() {
        scope.launch {
            while (true) {
                delay(CLEANUP_INTERVAL_MS)
                
                if (allowTimeout) {
                    cleanupStaleCircuits()
                }
            }
        }
    }
    
    /**
     * Remove stale or timed out circuits
     */
    private suspend fun cleanupStaleCircuits() = circuitMutex.withLock {
        val toRemove = mutableListOf<NetworkHost>()
        
        for ((host, circuit) in circuits) {
            when (circuit.state.value) {
                is CircuitState.Timeout, CircuitState.Closed -> {
                    toRemove.add(host)
                }
                else -> {
                    // Check if circuit has been inactive too long
                    val lastReceive = circuit.statistics.lastReceiveTime.get()
                    val inactive = System.currentTimeMillis() - lastReceive
                    
                    if (inactive > 300_000) { // 5 minutes
                        toRemove.add(host)
                        println("Removing inactive circuit for $host (${inactive}ms inactive)")
                    }
                }
            }
        }
        
        toRemove.forEach { host ->
            circuits.remove(host)?.close()
        }
        
        if (toRemove.isNotEmpty()) {
            println("Cleaned up ${toRemove.size} stale circuits")
        }
    }
    
    /**
     * Print comprehensive circuit status
     */
    fun printCircuitStatus() {
        val stats = calculateGlobalStats()
        
        println("""
            === Circuit Manager Status ===
            Total Circuits: ${stats.totalCircuits}
            Active: ${stats.activeCircuits}
            Degraded: ${stats.degradedCircuits}
            Blocked: ${stats.blockedCircuits}
            Timeout: ${stats.timeoutCircuits}
            
            Global Statistics:
            - Packets Out: ${stats.totalPacketsOut}
            - Packets In: ${stats.totalPacketsIn}
            - Packets Lost: ${stats.totalPacketsLost}
            - Loss Rate: ${"%.2f".format(stats.packetLossPercentage)}%
            - Bytes Out: ${stats.totalBytesOut}
            - Bytes In: ${stats.totalBytesIn}
            - Average Ping: ${"%.2f".format(stats.averagePing)}ms
            - Unacked Packets: ${stats.totalUnackedPackets}
        """.trimIndent())
        
        // Print individual circuit details
        println("\n=== Individual Circuits ===")
        circuits.forEach { (host, circuit) ->
            println("$host: ${circuit.state.value} (${circuit.getUnackedCount()} unacked)")
        }
    }
    
    /**
     * Shutdown circuit manager and cleanup all circuits
     */
    suspend fun shutdown() {
        circuits.values.forEach { it.close() }
        circuits.clear()
        scope.cancel()
        println("Circuit manager shutdown complete")
    }
    
    // Configuration
    fun setAllowTimeout(allow: Boolean) { allowTimeout = allow }
    fun setMaxCircuits(max: Int) { maxCircuits = max }
    
    fun getAllowTimeout(): Boolean = allowTimeout
    fun getMaxCircuits(): Int = maxCircuits
    fun getCircuitCount(): Int = circuits.size
}

/**
 * Global circuit statistics
 */
data class GlobalCircuitStats(
    val totalCircuits: Int,
    val activeCircuits: Int,
    val degradedCircuits: Int,
    val blockedCircuits: Int,
    val timeoutCircuits: Int,
    val totalPacketsOut: Long,
    val totalPacketsIn: Long,
    val totalPacketsLost: Long,
    val totalBytesOut: Long,
    val totalBytesIn: Long,
    val averagePing: Double,
    val totalUnackedPackets: Int
) {
    val packetLossPercentage: Double
        get() = if (totalPacketsOut > 0) {
            (totalPacketsLost.toDouble() / totalPacketsOut.toDouble()) * 100.0
        } else 0.0
    
    val overallHealth: String
        get() = when {
            packetLossPercentage < 1.0 && averagePing < 100.0 -> "Excellent"
            packetLossPercentage < 5.0 && averagePing < 500.0 -> "Good"
            packetLossPercentage < 10.0 && averagePing < 1000.0 -> "Fair"
            else -> "Poor"
        }
}

/**
 * Throttle manager for bandwidth control
 */
class ThrottleManager {
    private val hostThrottles = ConcurrentHashMap<NetworkHost, HostThrottle>()
    
    fun canSend(host: NetworkHost, bytes: Int): Boolean {
        val throttle = hostThrottles.getOrPut(host) { HostThrottle() }
        return throttle.canSend(bytes)
    }
    
    private class HostThrottle {
        private var lastResetTime = System.currentTimeMillis()
        private var bytesThisSecond = AtomicInteger(0)
        private val maxBytesPerSecond = 1024 * 1024 // 1MB/s
        
        fun canSend(bytes: Int): Boolean {
            val now = System.currentTimeMillis()
            
            if (now - lastResetTime >= 1000) {
                // Reset for new second
                bytesThisSecond.set(0)
                lastResetTime = now
            }
            
            val currentBytes = bytesThisSecond.get()
            if (currentBytes + bytes <= maxBytesPerSecond) {
                bytesThisSecond.addAndGet(bytes)
                return true
            }
            
            return false
        }
    }
}

/**
 * Global circuit manager instance for compatibility
 */
object GlobalCircuitManager {
    private var instance: CircuitManagerTranslated? = null
    
    fun initialize(): CircuitManagerTranslated {
        return CircuitManagerTranslated().also { instance = it }
    }
    
    fun getInstance(): CircuitManagerTranslated? = instance
    
    suspend fun shutdown() {
        instance?.shutdown()
        instance = null
    }
}

/**
 * Extension functions for easier circuit management
 */
suspend fun CircuitManagerTranslated.sendReliablePacket(
    host: NetworkHost,
    data: ByteArray,
    reliability: ReliabilityLevel = ReliabilityLevel.RELIABLE
): UInt? {
    val sequence = nextPacketSequence(host) ?: return null
    val packet = PacketBuffer(sequence, data, reliability)
    
    addReliablePacket(host, packet)
    return sequence
}

suspend fun CircuitManagerTranslated.isCircuitHealthy(host: NetworkHost): Boolean {
    return findCircuit(host)?.statistics?.isHealthy ?: false
}

fun CircuitManagerTranslated.monitorCircuitHealth(host: NetworkHost): Flow<Boolean> {
    return findCircuit(host)?.healthFlow ?: flowOf(false)
}