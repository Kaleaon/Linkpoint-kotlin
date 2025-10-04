/**
 * @file llcircuit.cpp
 * @brief Network circuit management for reliable UDP messaging
 * 
 * This handles the reliability layer on top of UDP for SecondLife messaging.
 * It manages packet acknowledgments, retransmissions, timeouts, and circuit state.
 * 
 * Original from SecondLife viewer (C++):
 * https://github.com/secondlife/viewer/blob/main/indra/llmessage/llcircuit.cpp
 * Translated to modern C++ with better practices.
 */

#include "llcircuit.h"
#include "llhost.h"
#include "llpacketbuffer.h"
#include "llthrottle.h"

#include <iostream>
#include <chrono>
#include <algorithm>
#include <unordered_map>
#include <queue>
#include <mutex>
#include <atomic>

class LLCircuitData {
public:
    LLHost mHost;
    bool mAlive;
    bool mBlocked;
    
    // Packet tracking
    U32 mPacketsOut;
    U32 mPacketsIn;
    U32 mPacketsLost;
    U32 mPacketLoss;
    
    // Timing
    std::chrono::steady_clock::time_point mLastReceiveTime;
    std::chrono::steady_clock::time_point mLastSendTime;
    F32 mPingDelay;
    F32 mPingDelayAveraged;
    
    // Sequence numbers
    U32 mNextOutgoingSequence;
    U32 mNextIncomingSequence;
    U32 mOldestUnackedPacket;
    
    // Reliable packet management
    std::map<U32, std::unique_ptr<LLPacketBuffer>> mUnackedPackets;
    std::queue<std::unique_ptr<LLPacketBuffer>> mRetryQueue;
    
    // Throttling
    std::unique_ptr<LLThrottleGroup> mThrottles;
    
    LLCircuitData(const LLHost& host) 
        : mHost(host)
        , mAlive(true)
        , mBlocked(false)
        , mPacketsOut(0)
        , mPacketsIn(0)
        , mPacketsLost(0)
        , mPacketLoss(0)
        , mLastReceiveTime(std::chrono::steady_clock::now())
        , mLastSendTime(std::chrono::steady_clock::now())
        , mPingDelay(0.0f)
        , mPingDelayAveraged(0.0f)
        , mNextOutgoingSequence(0)
        , mNextIncomingSequence(0)
        , mOldestUnackedPacket(0)
        , mThrottles(std::make_unique<LLThrottleGroup>())
    {
        // Initialize throttle categories
        mThrottles->resetDynamicAdjust();
    }
    
    ~LLCircuitData() = default;
    
    void checkPacketInID(U32 id, bool receive_resent) {
        auto now = std::chrono::steady_clock::now();
        mLastReceiveTime = now;
        mPacketsIn++;
        
        if (id > mNextIncomingSequence) {
            // Missing packets - estimate loss
            U32 missing = id - mNextIncomingSequence;
            mPacketsLost += missing;
            mNextIncomingSequence = id + 1;
        } else if (id == mNextIncomingSequence) {
            mNextIncomingSequence++;
        }
        // else: duplicate or out-of-order packet
        
        // Update packet loss statistics
        if (mPacketsIn > 0) {
            mPacketLoss = (mPacketsLost * 100) / (mPacketsIn + mPacketsLost);
        }
    }
    
    U32 nextPacketOutID() {
        mLastSendTime = std::chrono::steady_clock::now();
        mPacketsOut++;
        return mNextOutgoingSequence++;
    }
    
    void addReliablePacket(U32 packet_id, LLPacketBuffer* packet) {
        if (!packet) return;
        
        auto buffer = std::make_unique<LLPacketBuffer>(*packet);
        buffer->setSequence(packet_id);
        buffer->setSentTime(std::chrono::steady_clock::now());
        
        mUnackedPackets[packet_id] = std::move(buffer);
        
        // Update oldest unacked
        if (mUnackedPackets.size() == 1) {
            mOldestUnackedPacket = packet_id;
        }
    }
    
    void ackReliablePacket(U32 packet_id) {
        auto it = mUnackedPackets.find(packet_id);
        if (it != mUnackedPackets.end()) {
            // Calculate ping time
            auto now = std::chrono::steady_clock::now();
            auto sent_time = it->second->getSentTime();
            auto ping = std::chrono::duration<float>(now - sent_time).count();
            
            // Update ping statistics
            if (mPingDelayAveraged == 0.0f) {
                mPingDelayAveraged = ping;
            } else {
                mPingDelayAveraged = (mPingDelayAveraged * 0.95f) + (ping * 0.05f);
            }
            mPingDelay = ping;
            
            mUnackedPackets.erase(it);
            
            // Update oldest unacked
            if (!mUnackedPackets.empty()) {
                mOldestUnackedPacket = mUnackedPackets.begin()->first;
            }
        }
    }
    
    void checkForTimeouts(F32 timeout_seconds) {
        auto now = std::chrono::steady_clock::now();
        auto timeout_duration = std::chrono::duration<float>(timeout_seconds);
        
        for (auto it = mUnackedPackets.begin(); it != mUnackedPackets.end();) {
            auto& packet = it->second;
            if (now - packet->getSentTime() > timeout_duration) {
                // Timeout - move to retry queue
                packet->incrementRetryCount();
                
                if (packet->getRetryCount() < 3) {
                    mRetryQueue.push(std::move(packet));
                } else {
                    // Give up on this packet
                    mPacketsLost++;
                    std::cout << "Giving up on packet " << it->first 
                             << " to " << mHost.getString() << std::endl;
                }
                
                it = mUnackedPackets.erase(it);
            } else {
                ++it;
            }
        }
        
        // Check if circuit is alive
        auto time_since_last = now - mLastReceiveTime;
        if (time_since_last > std::chrono::seconds(60)) {
            mAlive = false;
            std::cout << "Circuit to " << mHost.getString() << " timed out" << std::endl;
        }
    }
    
    bool isAlive() const { return mAlive; }
    bool isBlocked() const { return mBlocked; }
    
    void setAlive(bool alive) { mAlive = alive; }
    void setBlocked(bool blocked) { mBlocked = blocked; }
    
    F32 getPingDelay() const { return mPingDelay; }
    F32 getPingDelayAveraged() const { return mPingDelayAveraged; }
    
    U32 getPacketsOut() const { return mPacketsOut; }
    U32 getPacketsIn() const { return mPacketsIn; }
    U32 getPacketsLost() const { return mPacketsLost; }
    U32 getPacketLoss() const { return mPacketLoss; }
    
    size_t getUnackedPacketCount() const { return mUnackedPackets.size(); }
    
    LLThrottleGroup& getThrottles() { return *mThrottles; }
    
    bool hasRetries() const { return !mRetryQueue.empty(); }
    
    std::unique_ptr<LLPacketBuffer> getNextRetry() {
        if (mRetryQueue.empty()) return nullptr;
        
        auto packet = std::move(mRetryQueue.front());
        mRetryQueue.pop();
        return packet;
    }
};

class LLCircuit {
private:
    std::unordered_map<LLHost, std::unique_ptr<LLCircuitData>> mCircuitData;
    mutable std::mutex mCircuitMutex;
    
    // Global settings
    F32 mTimeoutSeconds;
    bool mAllowTimeout;
    U32 mMaxCircuits;
    
public:
    LLCircuit() 
        : mTimeoutSeconds(5.0f)
        , mAllowTimeout(true) 
        , mMaxCircuits(256)
    {}
    
    ~LLCircuit() = default;
    
    LLCircuitData* findCircuit(const LLHost& host) {
        std::lock_guard<std::mutex> lock(mCircuitMutex);
        auto it = mCircuitData.find(host);
        return (it != mCircuitData.end()) ? it->second.get() : nullptr;
    }
    
    LLCircuitData* addCircuit(const LLHost& host) {
        std::lock_guard<std::mutex> lock(mCircuitMutex);
        
        // Check if we're at max circuits
        if (mCircuitData.size() >= mMaxCircuits) {
            std::cout << "Maximum circuits reached, cannot add " << host.getString() << std::endl;
            return nullptr;
        }
        
        auto circuit = std::make_unique<LLCircuitData>(host);
        auto* circuit_ptr = circuit.get();
        
        mCircuitData[host] = std::move(circuit);
        
        std::cout << "Added circuit to " << host.getString() << std::endl;
        return circuit_ptr;
    }
    
    void removeCircuit(const LLHost& host) {
        std::lock_guard<std::mutex> lock(mCircuitMutex);
        
        auto it = mCircuitData.find(host);
        if (it != mCircuitData.end()) {
            std::cout << "Removed circuit to " << host.getString() << std::endl;
            mCircuitData.erase(it);
        }
    }
    
    void checkForTimeouts() {
        std::lock_guard<std::mutex> lock(mCircuitMutex);
        
        if (!mAllowTimeout) return;
        
        for (auto it = mCircuitData.begin(); it != mCircuitData.end();) {
            auto& circuit = it->second;
            circuit->checkForTimeouts(mTimeoutSeconds);
            
            if (!circuit->isAlive()) {
                std::cout << "Removing dead circuit to " << it->first.getString() << std::endl;
                it = mCircuitData.erase(it);
            } else {
                ++it;
            }
        }
    }
    
    void ackReliablePacket(const LLHost& host, U32 packet_id) {
        auto* circuit = findCircuit(host);
        if (circuit) {
            circuit->ackReliablePacket(packet_id);
        }
    }
    
    void addReliablePacket(const LLHost& host, U32 packet_id, LLPacketBuffer* packet) {
        auto* circuit = findCircuit(host);
        if (!circuit) {
            circuit = addCircuit(host);
        }
        
        if (circuit) {
            circuit->addReliablePacket(packet_id, packet);
        }
    }
    
    U32 nextPacketID(const LLHost& host) {
        auto* circuit = findCircuit(host);
        if (!circuit) {
            circuit = addCircuit(host);
        }
        
        return circuit ? circuit->nextPacketOutID() : 0;
    }
    
    void checkPacketIn(const LLHost& host, U32 packet_id, bool receive_resent = false) {
        auto* circuit = findCircuit(host);
        if (!circuit) {
            circuit = addCircuit(host);
        }
        
        if (circuit) {
            circuit->checkPacketInID(packet_id, receive_resent);
        }
    }
    
    size_t getNumCircuits() const {
        std::lock_guard<std::mutex> lock(mCircuitMutex);
        return mCircuitData.size();
    }
    
    void dumpCircuits() const {
        std::lock_guard<std::mutex> lock(mCircuitMutex);
        
        std::cout << "=== Circuit Status Dump ===" << std::endl;
        std::cout << "Total circuits: " << mCircuitData.size() << std::endl;
        
        for (const auto& pair : mCircuitData) {
            const auto& host = pair.first;
            const auto& circuit = pair.second;
            
            std::cout << "Circuit to " << host.getString() << ":" << std::endl;
            std::cout << "  Alive: " << (circuit->isAlive() ? "Yes" : "No") << std::endl;
            std::cout << "  Blocked: " << (circuit->isBlocked() ? "Yes" : "No") << std::endl;
            std::cout << "  Packets Out: " << circuit->getPacketsOut() << std::endl;
            std::cout << "  Packets In: " << circuit->getPacketsIn() << std::endl;
            std::cout << "  Packets Lost: " << circuit->getPacketsLost() << std::endl;
            std::cout << "  Packet Loss: " << circuit->getPacketLoss() << "%" << std::endl;
            std::cout << "  Ping: " << circuit->getPingDelay() << "s" << std::endl;
            std::cout << "  Ping Avg: " << circuit->getPingDelayAveraged() << "s" << std::endl;
            std::cout << "  Unacked: " << circuit->getUnackedPacketCount() << std::endl;
            std::cout << std::endl;
        }
    }
    
    // Process retry queue for all circuits
    void processRetries() {
        std::lock_guard<std::mutex> lock(mCircuitMutex);
        
        for (auto& pair : mCircuitData) {
            auto& circuit = pair.second;
            
            while (circuit->hasRetries()) {
                auto retry_packet = circuit->getNextRetry();
                if (retry_packet) {
                    // Resend the packet
                    std::cout << "Retrying packet to " << pair.first.getString() << std::endl;
                    // Would call actual send function here
                }
            }
        }
    }
    
    void setTimeoutSeconds(F32 timeout) { mTimeoutSeconds = timeout; }
    void setAllowTimeout(bool allow) { mAllowTimeout = allow; }
    void setMaxCircuits(U32 max_circuits) { mMaxCircuits = max_circuits; }
    
    F32 getTimeoutSeconds() const { return mTimeoutSeconds; }
    bool getAllowTimeout() const { return mAllowTimeout; }
    U32 getMaxCircuits() const { return mMaxCircuits; }
    
    // Statistics
    struct CircuitStats {
        U32 total_circuits = 0;
        U32 alive_circuits = 0;
        U32 blocked_circuits = 0;
        U32 total_packets_out = 0;
        U32 total_packets_in = 0;
        U32 total_packets_lost = 0;
        F32 average_ping = 0.0f;
        size_t total_unacked = 0;
    };
    
    CircuitStats getGlobalStats() const {
        std::lock_guard<std::mutex> lock(mCircuitMutex);
        CircuitStats stats;
        
        stats.total_circuits = mCircuitData.size();
        
        F32 total_ping = 0.0f;
        U32 ping_count = 0;
        
        for (const auto& pair : mCircuitData) {
            const auto& circuit = pair.second;
            
            if (circuit->isAlive()) {
                stats.alive_circuits++;
            }
            
            if (circuit->isBlocked()) {
                stats.blocked_circuits++;
            }
            
            stats.total_packets_out += circuit->getPacketsOut();
            stats.total_packets_in += circuit->getPacketsIn();
            stats.total_packets_lost += circuit->getPacketsLost();
            stats.total_unacked += circuit->getUnackedPacketCount();
            
            F32 ping = circuit->getPingDelayAveraged();
            if (ping > 0.0f) {
                total_ping += ping;
                ping_count++;
            }
        }
        
        if (ping_count > 0) {
            stats.average_ping = total_ping / ping_count;
        }
        
        return stats;
    }
    
    void printStats() const {
        auto stats = getGlobalStats();
        
        std::cout << "=== Circuit Statistics ===" << std::endl;
        std::cout << "Total Circuits: " << stats.total_circuits << std::endl;
        std::cout << "Alive Circuits: " << stats.alive_circuits << std::endl;
        std::cout << "Blocked Circuits: " << stats.blocked_circuits << std::endl;
        std::cout << "Total Packets Out: " << stats.total_packets_out << std::endl;
        std::cout << "Total Packets In: " << stats.total_packets_in << std::endl;
        std::cout << "Total Packets Lost: " << stats.total_packets_lost << std::endl;
        std::cout << "Average Ping: " << stats.average_ping << "s" << std::endl;
        std::cout << "Total Unacked: " << stats.total_unacked << std::endl;
    }
};

// Global circuit instance
std::unique_ptr<LLCircuit> gCircuit;

void initializeCircuits() {
    gCircuit = std::make_unique<LLCircuit>();
    std::cout << "Circuit system initialized" << std::endl;
}

void shutdownCircuits() {
    if (gCircuit) {
        gCircuit->dumpCircuits();
        gCircuit.reset();
        std::cout << "Circuit system shutdown" << std::endl;
    }
}

LLCircuit* getCircuit() {
    return gCircuit.get();
}