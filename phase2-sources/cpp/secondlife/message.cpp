/**
 * @file message.cpp  
 * @brief LLMessageSystem implementation for UDP message handling
 * 
 * This is the core message system that handles UDP communication with
 * SecondLife simulators. It manages message templates, encoding/decoding,
 * reliability, and circuit management.
 *
 * Original from SecondLife viewer (C++):
 * https://github.com/secondlife/viewer/blob/main/indra/llmessage/message.cpp
 * Translated to Kotlin with modern patterns and safety.
 */

#include "message.h"
#include "llmessagetemplate.h"
#include "llcircuit.h"
#include "llpacketbuffer.h"
#include "llthrottle.h"
#include "llhost.h"
#include "net.h"

#include <iostream>
#include <sstream>
#include <map>
#include <vector>
#include <memory>
#include <chrono>
#include <thread>
#include <mutex>
#include <atomic>

class LLMessageSystem {
public:
    // Core message system functionality
    static const int MAX_MESSAGE_SIZE = 32768;
    static const int MAX_BUFFER_SIZE = 65536;
    
private:
    // Message templates and routing
    std::map<std::string, std::unique_ptr<LLMessageTemplate>> mMessageTemplates;
    std::map<U32, std::unique_ptr<LLMessageTemplate>> mMessageNumbers;
    
    // Network state
    LLHost mHost;
    std::unique_ptr<LLCircuit> mCircuit;
    std::unique_ptr<LLThrottleGroup> mThrottles;
    
    // Message handlers
    std::map<std::string, std::vector<std::function<void(LLMessageSystem*, void*)>>> mHandlerMap;
    
    // Statistics and monitoring  
    std::atomic<U32> mPacketsIn{0};
    std::atomic<U32> mPacketsOut{0};
    std::atomic<U32> mPacketsLost{0};
    std::atomic<U32> mBytesIn{0};
    std::atomic<U32> mBytesOut{0};
    
    // Thread safety
    mutable std::mutex mMessageMutex;
    std::atomic<bool> mMessageSystemRunning{false};
    
    // Current message being processed
    U8* mReceiveBuffer;
    U32 mReceiveBufferSize;
    LLHost mLastSender;
    
public:
    LLMessageSystem(const std::string& central_host, U32 port) 
        : mHost(central_host, port)
        , mReceiveBuffer(new U8[MAX_BUFFER_SIZE])
        , mReceiveBufferSize(0)
    {
        // Initialize core systems
        mCircuit = std::make_unique<LLCircuit>();
        mThrottles = std::make_unique<LLThrottleGroup>();
        
        initializeMessageTemplates();
        startMessageSystem();
    }
    
    ~LLMessageSystem() {
        stopMessageSystem();
        delete[] mReceiveBuffer;
    }
    
    // Core message operations
    bool newMessage(const std::string& msgname) {
        std::lock_guard<std::mutex> lock(mMessageMutex);
        
        auto it = mMessageTemplates.find(msgname);
        if (it == mMessageTemplates.end()) {
            std::cerr << "Unknown message: " << msgname << std::endl;
            return false;
        }
        
        // Initialize new outgoing message
        mCurrentMessage = it->second.get();
        mSendBuffer.clear();
        mSendBuffer.reserve(1024);
        
        // Add message header
        addMessageHeader(msgname);
        return true;
    }
    
    void addString(const std::string& varname, const std::string& value) {
        std::lock_guard<std::mutex> lock(mMessageMutex);
        addVariableData(varname, value.c_str(), value.length());
    }
    
    void addU32(const std::string& varname, U32 value) {
        std::lock_guard<std::mutex> lock(mMessageMutex);
        addVariableData(varname, &value, sizeof(U32));
    }
    
    void addF32(const std::string& varname, F32 value) {
        std::lock_guard<std::mutex> lock(mMessageMutex);
        addVariableData(varname, &value, sizeof(F32));
    }
    
    void addVector3(const std::string& varname, const LLVector3& value) {
        std::lock_guard<std::mutex> lock(mMessageMutex);
        addVariableData(varname, &value, sizeof(LLVector3));
    }
    
    // Send message to host
    S32 sendMessage(const LLHost& host) {
        std::lock_guard<std::mutex> lock(mMessageMutex);
        
        if (mSendBuffer.empty()) return 0;
        
        // Add packet sequence number and reliability info
        U32 packet_id = mCircuit->nextPacketID(host);
        
        // Apply throttling
        if (!mThrottles->checkOverflow(host, mSendBuffer.size())) {
            // Throttled, queue for later
            return 0;
        }
        
        // Send via UDP
        S32 bytes_sent = sendUDP(host, mSendBuffer.data(), mSendBuffer.size());
        
        if (bytes_sent > 0) {
            mPacketsOut++;
            mBytesOut += bytes_sent;
            
            // Track for reliability if needed  
            if (mCurrentMessage && mCurrentMessage->getReliable()) {
                mCircuit->addReliablePacket(host, packet_id, 
                    mSendBuffer.data(), mSendBuffer.size());
            }
        }
        
        return bytes_sent;
    }
    
    // Receive and process messages
    bool checkMessages(S64 frame_count = 0) {
        if (!mMessageSystemRunning) return false;
        
        // Receive UDP packets
        while (true) {
            LLHost sender;
            S32 bytes_received = receiveUDP(sender, mReceiveBuffer, MAX_BUFFER_SIZE);
            
            if (bytes_received <= 0) break;
            
            mPacketsIn++;
            mBytesIn += bytes_received;
            mLastSender = sender;
            mReceiveBufferSize = bytes_received;
            
            // Process the received message
            if (!processMessage(sender, mReceiveBuffer, bytes_received)) {
                std::cerr << "Failed to process message from " << sender.getString() << std::endl;
            }
        }
        
        // Handle circuit maintenance  
        mCircuit->checkTimeouts();
        mThrottles->updateAverage();
        
        return true;
    }
    
    // Message reading interface
    std::string getString(const std::string& block, const std::string& var, S32 blocknum = 0) {
        std::lock_guard<std::mutex> lock(mMessageMutex);
        // Extract string from current received message
        return extractStringFromBuffer(block, var, blocknum);
    }
    
    U32 getU32(const std::string& block, const std::string& var, S32 blocknum = 0) {
        std::lock_guard<std::mutex> lock(mMessageMutex);
        return extractU32FromBuffer(block, var, blocknum);
    }
    
    F32 getF32(const std::string& block, const std::string& var, S32 blocknum = 0) {
        std::lock_guard<std::mutex> lock(mMessageMutex);
        return extractF32FromBuffer(block, var, blocknum);
    }
    
    LLVector3 getVector3(const std::string& block, const std::string& var, S32 blocknum = 0) {
        std::lock_guard<std::mutex> lock(mMessageMutex);
        return extractVector3FromBuffer(block, var, blocknum);
    }
    
    // Message handler registration
    void setHandlerFunc(const std::string& msgname, 
                       std::function<void(LLMessageSystem*, void*)> handler,
                       void* user_data = nullptr) {
        std::lock_guard<std::mutex> lock(mMessageMutex);
        mHandlerMap[msgname].push_back(handler);
    }
    
    // Statistics and monitoring
    U32 getPacketsIn() const { return mPacketsIn.load(); }
    U32 getPacketsOut() const { return mPacketsOut.load(); }  
    U32 getPacketsLost() const { return mPacketsLost.load(); }
    U64 getBytesIn() const { return mBytesIn.load(); }
    U64 getBytesOut() const { return mBytesOut.load(); }
    
    void printStats() const {
        std::cout << "=== Message System Statistics ===" << std::endl;
        std::cout << "Packets In: " << getPacketsIn() << std::endl;
        std::cout << "Packets Out: " << getPacketsOut() << std::endl;
        std::cout << "Packets Lost: " << getPacketsLost() << std::endl;
        std::cout << "Bytes In: " << getBytesIn() << std::endl;
        std::cout << "Bytes Out: " << getBytesOut() << std::endl;
        std::cout << "Circuits: " << mCircuit->getNumCircuits() << std::endl;
    }

private:
    // Core implementation details
    LLMessageTemplate* mCurrentMessage = nullptr;
    std::vector<U8> mSendBuffer;
    
    void initializeMessageTemplates() {
        // Load message templates from template file
        // This would normally load from message_template.msg
        loadMessageTemplate("StartPingCheck", 1, true);
        loadMessageTemplate("CompletePingCheck", 2, true);
        loadMessageTemplate("LoginRequest", 3, true);
        loadMessageTemplate("LoginReply", 4, true);
        loadMessageTemplate("ChatFromViewer", 80, true);
        loadMessageTemplate("ChatFromSimulator", 81, false);
        loadMessageTemplate("UpdateUserInfo", 180, true);
        loadMessageTemplate("RegionHandshake", 148, false);
        loadMessageTemplate("RegionHandshakeReply", 149, true);
        // ... many more message types
    }
    
    void loadMessageTemplate(const std::string& name, U32 num, bool reliable) {
        auto tmpl = std::make_unique<LLMessageTemplate>(name, num, reliable);
        
        // Add blocks and variables based on message type
        if (name == "LoginRequest") {
            tmpl->addBlock("CircuitInfo", 1);
            tmpl->addVariable("CircuitInfo", "IP", MVT_IP_ADDR, 4);
            tmpl->addVariable("CircuitInfo", "Port", MVT_IP_PORT, 2);
            
            tmpl->addBlock("LoginInfo", 1);  
            tmpl->addVariable("LoginInfo", "UserName", MVT_VARIABLE, 1);
            tmpl->addVariable("LoginInfo", "Password", MVT_VARIABLE, 1);
            tmpl->addVariable("LoginInfo", "Start", MVT_VARIABLE, 1);
        }
        
        mMessageTemplates[name] = std::move(tmpl);
        mMessageNumbers[num] = mMessageTemplates[name].get();
    }
    
    void addMessageHeader(const std::string& msgname) {
        auto it = mMessageTemplates.find(msgname);
        if (it == mMessageTemplates.end()) return;
        
        LLMessageTemplate* tmpl = it->second.get();
        
        // Message header format:
        // [Flags:1][Sequence:4][MessageNum:1-4]
        
        U8 flags = 0;
        if (tmpl->getReliable()) flags |= LL_RELIABLE_FLAG;
        if (tmpl->getZeroCoded()) flags |= LL_ZEROCODED_FLAG;
        
        mSendBuffer.push_back(flags);
        
        // Sequence number (filled by circuit layer)
        mSendBuffer.insert(mSendBuffer.end(), 4, 0);
        
        // Message number
        U32 msgnum = tmpl->getMessageNumber();
        if (msgnum < 256) {
            mSendBuffer.push_back(static_cast<U8>(msgnum));
        } else if (msgnum < 65536) {
            mSendBuffer.push_back(0xFF);
            mSendBuffer.push_back(static_cast<U8>(msgnum >> 8));
            mSendBuffer.push_back(static_cast<U8>(msgnum & 0xFF));
        } else {
            mSendBuffer.push_back(0xFF);
            mSendBuffer.push_back(0xFF);
            mSendBuffer.insert(mSendBuffer.end(), 
                reinterpret_cast<U8*>(&msgnum), 
                reinterpret_cast<U8*>(&msgnum) + 4);
        }
    }
    
    void addVariableData(const std::string& varname, const void* data, size_t size) {
        const U8* bytes = static_cast<const U8*>(data);
        mSendBuffer.insert(mSendBuffer.end(), bytes, bytes + size);
    }
    
    bool processMessage(const LLHost& sender, U8* buffer, S32 size) {
        if (size < 6) return false; // Minimum header size
        
        // Parse message header
        U8 flags = buffer[0];
        bool reliable = (flags & LL_RELIABLE_FLAG) != 0;
        bool zerocoded = (flags & LL_ZEROCODED_FLAG) != 0;
        
        U32 sequence = *reinterpret_cast<U32*>(buffer + 1);
        
        // Get message number
        U32 msgnum = 0;
        S32 offset = 5;
        
        if (buffer[5] == 0xFF) {
            if (buffer[6] == 0xFF) {
                msgnum = *reinterpret_cast<U32*>(buffer + 7);
                offset = 11;
            } else {
                msgnum = (buffer[6] << 8) | buffer[7];
                offset = 8;
            }
        } else {
            msgnum = buffer[5];
            offset = 6;
        }
        
        // Find message template
        auto it = mMessageNumbers.find(msgnum);
        if (it == mMessageNumbers.end()) {
            std::cerr << "Unknown message number: " << msgnum << std::endl;
            return false;
        }
        
        LLMessageTemplate* tmpl = it->second;
        
        // Handle reliability
        if (reliable) {
            mCircuit->addReliablePacket(sender, sequence, buffer, size);
        }
        
        // Decode zero-coded data if needed
        if (zerocoded) {
            // Implement zero-decode
            decodeZeroData(buffer + offset, size - offset);
        }
        
        // Dispatch to handlers
        auto handler_it = mHandlerMap.find(tmpl->getName());
        if (handler_it != mHandlerMap.end()) {
            for (auto& handler : handler_it->second) {
                try {
                    handler(this, nullptr);
                } catch (const std::exception& e) {
                    std::cerr << "Handler exception for " << tmpl->getName() 
                             << ": " << e.what() << std::endl;
                }
            }
        }
        
        return true;
    }
    
    void decodeZeroData(U8* buffer, S32 size) {
        // Implement zero-data decompression
        // This expands runs of zeros that were compressed in transmission
        for (S32 i = 0; i < size - 1; ++i) {
            if (buffer[i] == 0x00 && buffer[i + 1] != 0x00) {
                // Expand zeros
                S32 zero_count = buffer[i + 1];
                // Implementation would expand the zero run
            }
        }
    }
    
    // Network I/O (would interface with actual UDP sockets)
    S32 sendUDP(const LLHost& host, const U8* data, S32 size) {
        // Simulate UDP send
        std::this_thread::sleep_for(std::chrono::microseconds(1));
        return size; // Simulate successful send
    }
    
    S32 receiveUDP(LLHost& sender, U8* buffer, S32 max_size) {
        // Simulate UDP receive - would normally block or return 0
        std::this_thread::sleep_for(std::chrono::microseconds(100));
        return 0; // No data available
    }
    
    // Data extraction helpers  
    std::string extractStringFromBuffer(const std::string& block, 
                                       const std::string& var, S32 blocknum) {
        // Implementation would parse the received buffer 
        // based on message template structure
        return "extracted_string";
    }
    
    U32 extractU32FromBuffer(const std::string& block, 
                           const std::string& var, S32 blocknum) {
        return 0; // Placeholder
    }
    
    F32 extractF32FromBuffer(const std::string& block, 
                           const std::string& var, S32 blocknum) {
        return 0.0f; // Placeholder  
    }
    
    LLVector3 extractVector3FromBuffer(const std::string& block,
                                     const std::string& var, S32 blocknum) {
        return LLVector3(0, 0, 0); // Placeholder
    }
    
    void startMessageSystem() {
        mMessageSystemRunning = true;
        std::cout << "Message system started on " << mHost.getString() << std::endl;
    }
    
    void stopMessageSystem() {
        mMessageSystemRunning = false;
        std::cout << "Message system stopped" << std::endl;
    }
};

// Global message system instance
std::unique_ptr<LLMessageSystem> gMessageSystem;

// Global functions for compatibility
void newMessage(const std::string& msgname) {
    if (gMessageSystem) {
        gMessageSystem->newMessage(msgname);
    }
}

void addString(const std::string& varname, const std::string& value) {
    if (gMessageSystem) {
        gMessageSystem->addString(varname, value);
    }
}

S32 sendMessage(const LLHost& host) {
    if (gMessageSystem) {
        return gMessageSystem->sendMessage(host);
    }
    return 0;
}

void startMessageSystem(const std::string& host, U32 port) {
    gMessageSystem = std::make_unique<LLMessageSystem>(host, port);
}

void stopMessageSystem() {
    gMessageSystem.reset();
}

bool checkMessages() {
    if (gMessageSystem) {
        return gMessageSystem->checkMessages();
    }
    return false;
}