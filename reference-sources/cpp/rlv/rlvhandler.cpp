/**
 * @file rlvhandler.cpp
 * @brief Main RLV command processing
 * 
 * Original Restrained Love Viewer component
 * Source: https://github.com/RestrainedLove/RestrainedLove
 * This is a reference implementation for translation to Kotlin RLVProcessor
 */

#include "rlvhandler.h"
#include <iostream>
#include <string>
#include <vector>
#include <map>
#include <set>
#include <sstream>
#include <algorithm>

enum RLVCommandType {
    RLV_ATTACH = 1,
    RLV_DETACH = 2,
    RLV_ADDOUTFIT = 3,
    RLV_REMOUTFIT = 4,
    RLV_SHOWLOC = 5,
    RLV_SHOWNEARBY = 6,
    RLV_SHOWTAG = 7,
    RLV_SITTP = 8,
    RLV_TPLM = 9,
    RLV_TPLOC = 10
};

struct RLVCommand {
    std::string fullCommand;
    std::string behavior;
    std::string option;
    std::string param;
    bool force;
    std::string objectId;
    
    RLVCommand(const std::string& cmd, const std::string& objId) 
        : fullCommand(cmd), force(false), objectId(objId) {
        parseCommand();
    }
    
private:
    void parseCommand() {
        // Parse RLV command format: @behavior[:option]=param,behavior[:option]=param
        // Example: "@detach=n,tploc=n,sittp=force"
        
        if (fullCommand.empty() || fullCommand[0] != '@') {
            return;
        }
        
        std::string command = fullCommand.substr(1); // Remove '@'
        
        size_t equalPos = command.find('=');
        if (equalPos != std::string::npos) {
            std::string behaviorPart = command.substr(0, equalPos);
            param = command.substr(equalPos + 1);
            
            size_t colonPos = behaviorPart.find(':');
            if (colonPos != std::string::npos) {
                behavior = behaviorPart.substr(0, colonPos);
                option = behaviorPart.substr(colonPos + 1);
            } else {
                behavior = behaviorPart;
            }
            
            force = (param == "force");
        }
    }
};

class RlvHandler {
private:
    std::set<std::string> mRestrictions;
    std::map<std::string, std::set<std::string>> mObjectRestrictions;
    bool mEnabled;
    bool mInitialized;
    
public:
    RlvHandler() : mEnabled(true), mInitialized(false) {}
    
    /**
     * Initialize the RLV command processing system
     */
    bool init() {
        std::cout << "Initializing RlvHandler (Restrained Love Viewer protocol)..." << std::endl;
        
        mRestrictions.clear();
        mObjectRestrictions.clear();
        mEnabled = true;
        mInitialized = true;
        
        std::cout << "RlvHandler initialized - RLV protocol extensions ready" << std::endl;
        return true;
    }
    
    /**
     * Enable or disable RLV functionality
     */
    void setEnabled(bool enabled) {
        mEnabled = enabled;
        std::cout << "RLV " << (enabled ? "enabled" : "disabled") << std::endl;
        
        if (!enabled) {
            // Clear all restrictions when disabled
            clearAllRestrictions();
        }
    }
    
    /**
     * Process an RLV command from an object
     */
    bool processCommand(const std::string& command, const std::string& objectId) {
        if (!mInitialized || !mEnabled) {
            std::cout << "RLV command ignored - system not enabled" << std::endl;
            return false;
        }
        
        std::cout << "Processing RLV command: " << command << " from object " << objectId << std::endl;
        
        RLVCommand cmd(command, objectId);
        
        if (cmd.behavior.empty()) {
            std::cerr << "Invalid RLV command format" << std::endl;
            return false;
        }
        
        // Process the command based on behavior
        return executeCommand(cmd);
    }
    
    /**
     * Check if a specific behavior is restricted
     */
    bool isRestricted(const std::string& behavior, const std::string& option = "") {
        std::string fullBehavior = behavior;
        if (!option.empty()) {
            fullBehavior += ":" + option;
        }
        
        return mRestrictions.find(fullBehavior) != mRestrictions.end();
    }
    
    /**
     * Get all current restrictions
     */
    std::vector<std::string> getCurrentRestrictions() {
        std::vector<std::string> restrictions;
        for (const auto& restriction : mRestrictions) {
            restrictions.push_back(restriction);
        }
        return restrictions;
    }
    
    /**
     * Clear all restrictions from a specific object
     */
    void clearObjectRestrictions(const std::string& objectId) {
        auto it = mObjectRestrictions.find(objectId);
        if (it != mObjectRestrictions.end()) {
            // Remove restrictions from global set
            for (const auto& restriction : it->second) {
                mRestrictions.erase(restriction);
            }
            
            // Remove object entry
            mObjectRestrictions.erase(it);
            
            std::cout << "Cleared all RLV restrictions from object " << objectId << std::endl;
        }
    }
    
    /**
     * Clear all restrictions from all objects
     */
    void clearAllRestrictions() {
        mRestrictions.clear();
        mObjectRestrictions.clear();
        std::cout << "Cleared all RLV restrictions" << std::endl;
    }
    
private:
    bool executeCommand(const RLVCommand& cmd) {
        std::string fullBehavior = cmd.behavior;
        if (!cmd.option.empty()) {
            fullBehavior += ":" + cmd.option;
        }
        
        if (cmd.param == "n" || cmd.param == "add") {
            // Add restriction
            addRestriction(fullBehavior, cmd.objectId);
            return true;
        } else if (cmd.param == "y" || cmd.param == "rem") {
            // Remove restriction
            removeRestriction(fullBehavior, cmd.objectId);
            return true;
        } else if (cmd.param == "force") {
            // Force action (immediate execution)
            return executeForceCommand(cmd);
        }
        
        std::cerr << "Unknown RLV command parameter: " << cmd.param << std::endl;
        return false;
    }
    
    void addRestriction(const std::string& behavior, const std::string& objectId) {
        mRestrictions.insert(behavior);
        mObjectRestrictions[objectId].insert(behavior);
        
        std::cout << "  -> Added RLV restriction: " << behavior << " from object " << objectId << std::endl;
    }
    
    void removeRestriction(const std::string& behavior, const std::string& objectId) {
        mRestrictions.erase(behavior);
        
        auto it = mObjectRestrictions.find(objectId);
        if (it != mObjectRestrictions.end()) {
            it->second.erase(behavior);
            if (it->second.empty()) {
                mObjectRestrictions.erase(it);
            }
        }
        
        std::cout << "  -> Removed RLV restriction: " << behavior << " from object " << objectId << std::endl;
    }
    
    bool executeForceCommand(const RLVCommand& cmd) {
        std::cout << "  -> Executing RLV force command: " << cmd.behavior << std::endl;
        
        // Implement specific force behaviors
        if (cmd.behavior == "sittp") {
            std::cout << "    - Force sitting on object" << std::endl;
        } else if (cmd.behavior == "tplm") {
            std::cout << "    - Force teleport to landmark" << std::endl;
        } else if (cmd.behavior == "tploc") {
            std::cout << "    - Force teleport to location" << std::endl;
        } else if (cmd.behavior == "attach") {
            std::cout << "    - Force attach object" << std::endl;
        } else if (cmd.behavior == "detach") {
            std::cout << "    - Force detach object" << std::endl;
        }
        
        return true;
    }
};

// Global instance
static RlvHandler* gRlvHandler = nullptr;

/**
 * Initialize the global RLV system
 */
bool initRlvHandler() {
    if (!gRlvHandler) {
        gRlvHandler = new RlvHandler();
        return gRlvHandler->init();
    }
    return true;
}

/**
 * Get global RLV handler instance
 */
RlvHandler* getRlvHandler() {
    return gRlvHandler;
}

/**
 * Process RLV command
 */
bool processRlvCommand(const std::string& command, const std::string& objectId) {
    return gRlvHandler ? gRlvHandler->processCommand(command, objectId) : false;
}

/**
 * Check if behavior is restricted
 */
bool isRlvRestricted(const std::string& behavior, const std::string& option) {
    return gRlvHandler ? gRlvHandler->isRestricted(behavior, option) : false;
}

/**
 * Cleanup global RLV system
 */
void shutdownRlvHandler() {
    if (gRlvHandler) {
        delete gRlvHandler;
        gRlvHandler = nullptr;
        std::cout << "RlvHandler shut down" << std::endl;
    }
}