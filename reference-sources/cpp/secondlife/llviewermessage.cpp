/**
 * @file llviewermessage.cpp
 * @brief UDP message handling for simulator communication
 * 
 * Original SecondLife Viewer component
 * Source: https://github.com/secondlife/viewer
 * This is a reference implementation for translation to Kotlin MessageProcessor
 */

#include "llviewermessage.h"
#include <iostream>
#include <string>
#include <vector>
#include <map>
#include <functional>
#include <cstring>

enum MessageType {
    LOGIN_REQUEST = 1,
    LOGIN_RESPONSE = 2,
    LOGOUT_REQUEST = 3,
    AGENT_UPDATE = 4,
    OBJECT_UPDATE = 5,
    CHAT_MESSAGE = 6,
    INVENTORY_UPDATE = 7
};

struct MessageData {
    MessageType type;
    std::vector<uint8_t> payload;
    size_t size;
    
    MessageData(MessageType t, const void* data, size_t s) 
        : type(t), size(s) {
        payload.resize(s);
        if (data && s > 0) {
            std::memcpy(payload.data(), data, s);
        }
    }
};

class LLViewerMessage {
private:
    std::map<MessageType, std::function<void(const MessageData&)>> mHandlers;
    bool mInitialized;
    
public:
    LLViewerMessage() : mInitialized(false) {}
    
    /**
     * Initialize the message system
     */
    bool init() {
        std::cout << "Initializing LLViewerMessage system..." << std::endl;
        
        // Register default message handlers
        registerHandler(LOGIN_REQUEST, [this](const MessageData& msg) { handleLoginRequest(msg); });
        registerHandler(LOGIN_RESPONSE, [this](const MessageData& msg) { handleLoginResponse(msg); });
        registerHandler(LOGOUT_REQUEST, [this](const MessageData& msg) { handleLogoutRequest(msg); });
        registerHandler(AGENT_UPDATE, [this](const MessageData& msg) { handleAgentUpdate(msg); });
        registerHandler(OBJECT_UPDATE, [this](const MessageData& msg) { handleObjectUpdate(msg); });
        registerHandler(CHAT_MESSAGE, [this](const MessageData& msg) { handleChatMessage(msg); });
        registerHandler(INVENTORY_UPDATE, [this](const MessageData& msg) { handleInventoryUpdate(msg); });
        
        mInitialized = true;
        std::cout << "LLViewerMessage system initialized with " << mHandlers.size() << " handlers" << std::endl;
        return true;
    }
    
    /**
     * Register a message handler for a specific message type
     */
    void registerHandler(MessageType type, std::function<void(const MessageData&)> handler) {
        mHandlers[type] = handler;
        std::cout << "Registered handler for message type " << static_cast<int>(type) << std::endl;
    }
    
    /**
     * Process an incoming message
     */
    void processMessage(const uint8_t* data, size_t size) {
        if (!mInitialized || size < sizeof(MessageType)) {
            std::cerr << "Cannot process message - system not initialized or invalid size" << std::endl;
            return;
        }
        
        // Extract message type from first bytes
        MessageType type = static_cast<MessageType>(*reinterpret_cast<const int*>(data));
        
        // Create message data structure
        MessageData msg(type, data + sizeof(MessageType), size - sizeof(MessageType));
        
        // Find and call handler
        auto it = mHandlers.find(type);
        if (it != mHandlers.end()) {
            std::cout << "Processing message type " << static_cast<int>(type) << " (size: " << size << " bytes)" << std::endl;
            it->second(msg);
        } else {
            std::cerr << "No handler registered for message type " << static_cast<int>(type) << std::endl;
        }
    }
    
    /**
     * Send a message
     */
    bool sendMessage(MessageType type, const void* data, size_t size) {
        std::cout << "Sending message type " << static_cast<int>(type) << " (size: " << size << " bytes)" << std::endl;
        
        // In real implementation, this would serialize and send via UDP
        // For now, just simulate the send
        
        return true;
    }
    
private:
    void handleLoginRequest(const MessageData& msg) {
        std::cout << "  -> Handling LOGIN_REQUEST" << std::endl;
        // Parse login credentials, validate, create session
    }
    
    void handleLoginResponse(const MessageData& msg) {
        std::cout << "  -> Handling LOGIN_RESPONSE" << std::endl;
        // Process login success/failure, extract session info
    }
    
    void handleLogoutRequest(const MessageData& msg) {
        std::cout << "  -> Handling LOGOUT_REQUEST" << std::endl;
        // Clean up session, notify server of logout
    }
    
    void handleAgentUpdate(const MessageData& msg) {
        std::cout << "  -> Handling AGENT_UPDATE" << std::endl;
        // Update agent position, rotation, movement state
    }
    
    void handleObjectUpdate(const MessageData& msg) {
        std::cout << "  -> Handling OBJECT_UPDATE" << std::endl;
        // Update object properties, position, texture, etc.
    }
    
    void handleChatMessage(const MessageData& msg) {
        std::cout << "  -> Handling CHAT_MESSAGE" << std::endl;
        // Display chat message in UI, apply filtering
    }
    
    void handleInventoryUpdate(const MessageData& msg) {
        std::cout << "  -> Handling INVENTORY_UPDATE" << std::endl;
        // Update inventory items, folders, permissions
    }
};

// Global instance
static LLViewerMessage* gViewerMessage = nullptr;

/**
 * Initialize the global message system
 */
bool initViewerMessage() {
    if (!gViewerMessage) {
        gViewerMessage = new LLViewerMessage();
        return gViewerMessage->init();
    }
    return true;
}

/**
 * Get global message system instance
 */
LLViewerMessage* getViewerMessage() {
    return gViewerMessage;
}

/**
 * Cleanup global message system
 */
void shutdownViewerMessage() {
    if (gViewerMessage) {
        delete gViewerMessage;
        gViewerMessage = nullptr;
        std::cout << "LLViewerMessage system shut down" << std::endl;
    }
}