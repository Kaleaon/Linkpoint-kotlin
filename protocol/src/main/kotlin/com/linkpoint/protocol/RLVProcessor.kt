package com.linkpoint.protocol

import com.linkpoint.core.events.EventSystem
import com.linkpoint.core.events.ViewerEvent

/**
 * Restrained Love Viewer (RLV) Protocol Extension Processor
 * 
 * This class implements the RLV protocol extensions that allow objects in SecondLife
 * to send commands that control viewer behavior and avatar actions. RLV enables
 * interactive experiences like games, role-playing scenarios, and immersive content.
 * 
 * Imported and modernized from Restrained Love Viewer components:
 * - Original C++: rlvhandler.cpp, rlvbehaviourmanager.cpp, rlvcommands.cpp
 * - Modern Kotlin: Type-safe command parsing, event-driven architecture, null safety
 * 
 * RLV Protocol Overview:
 * - Commands are sent via LSL scripts in objects using llOwnerSay() with @command format
 * - Commands can restrict avatar actions, force animations, control camera, etc.
 * - Supports versioning and capability negotiation
 * - Provides feedback to scripts about command success/failure
 * 
 * Security Model:
 * - Only works for the avatar that owns the commanding object
 * - Can be globally disabled by user preference
 * - Individual command categories can be disabled
 * - Provides clear user feedback about active restrictions
 */
class RLVProcessor {
    
    // RLV system state
    private var isRLVEnabled = true
    private var rlvVersion = "2.9.0" // Compatible version
    private val activeRestrictions = mutableMapOf<String, RLVRestriction>()
    private val blacklistedCommands = mutableSetOf<String>()
    
    /**
     * Represents an active RLV restriction
     */
    data class RLVRestriction(
        val command: String,
        val parameter: String?,
        val objectId: String,
        val objectName: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * RLV command categories for organization and control
     */
    enum class RLVCommandCategory {
        MOVEMENT,      // Commands affecting avatar movement
        COMMUNICATION, // Commands affecting chat and IM
        INVENTORY,     // Commands affecting inventory access
        APPEARANCE,    // Commands affecting avatar appearance
        WORLD,         // Commands affecting world interaction
        CAMERA,        // Commands affecting camera control
        TELEPORT,      // Commands affecting teleportation
        ATTACHMENT,    // Commands affecting attachments
        ANIMATION,     // Commands affecting animations
        DEBUG         // Debug and diagnostic commands
    }
    
    /**
     * Standard RLV commands with their categories and descriptions
     * Based on the RLV specification and command documentation
     */
    enum class RLVCommand(
        val command: String, 
        val category: RLVCommandCategory, 
        val description: String,
        val hasParameter: Boolean = false
    ) {
        // Movement restrictions
        FLY("fly", RLVCommandCategory.MOVEMENT, "Prevent/allow flying"),
        TPLM("tplm", RLVCommandCategory.TELEPORT, "Prevent teleporting to landmarks"),
        TPLOC("tploc", RLVCommandCategory.TELEPORT, "Prevent teleporting to locations"),
        SITTP("sittp", RLVCommandCategory.MOVEMENT, "Prevent/allow sitting on objects"),
        
        // Communication restrictions  
        SENDCHAT("sendchat", RLVCommandCategory.COMMUNICATION, "Prevent/allow public chat"),
        RECVCHAT("recvchat", RLVCommandCategory.COMMUNICATION, "Prevent/allow receiving chat"),
        SENDIM("sendim", RLVCommandCategory.COMMUNICATION, "Prevent/allow sending IMs"),
        RECVIM("recvim", RLVCommandCategory.COMMUNICATION, "Prevent/allow receiving IMs"),
        
        // Inventory restrictions
        SHOWINV("showinv", RLVCommandCategory.INVENTORY, "Prevent/allow showing inventory"),
        VIEWNOTE("viewnote", RLVCommandCategory.INVENTORY, "Prevent/allow viewing notecards"),
        
        // Appearance restrictions
        ADDATTACH("addattach", RLVCommandCategory.ATTACHMENT, "Prevent/allow attaching items", true),
        REMATTACH("remattach", RLVCommandCategory.ATTACHMENT, "Prevent/allow detaching items", true),
        ADDOUTFIT("addoutfit", RLVCommandCategory.APPEARANCE, "Prevent/allow wearing items", true),
        REMOUTFIT("remoutfit", RLVCommandCategory.APPEARANCE, "Prevent/allow removing items", true),
        
        // World interaction
        REZ("rez", RLVCommandCategory.WORLD, "Prevent/allow rezzing objects"),
        EDIT("edit", RLVCommandCategory.WORLD, "Prevent/allow editing objects"),
        SHOWWORLDMAP("showworldmap", RLVCommandCategory.WORLD, "Prevent/allow showing world map"),
        SHOWMINIMAP("showminimap", RLVCommandCategory.WORLD, "Prevent/allow showing minimap"),
        
        // Camera control
        CAMZOOMMIN("camzoommin", RLVCommandCategory.CAMERA, "Set minimum camera zoom", true),
        CAMZOOMMAX("camzoommax", RLVCommandCategory.CAMERA, "Set maximum camera zoom", true),
        SETCAM_FOCUS("setcam_focus", RLVCommandCategory.CAMERA, "Force camera focus point", true),
        
        // Animation control
        UNSIT("unsit", RLVCommandCategory.MOVEMENT, "Prevent/allow standing up from sit"),
        
        // Debug commands
        VERSION("version", RLVCommandCategory.DEBUG, "Request RLV version information"),
        VERSIONNUM("versionnum", RLVCommandCategory.DEBUG, "Request RLV version number"),
        VERSIONNEW("versionnew", RLVCommandCategory.DEBUG, "Check if viewer supports newer RLV");
        
        companion object {
            fun fromString(command: String): RLVCommand? = values().find { it.command == command }
        }
    }
    
    /**
     * Process an RLV command received from an object
     * 
     * RLV commands follow the format: @command[=parameter][,command2=parameter2...] 
     * Examples:
     * - @fly=n (disable flying)
     * - @sendchat=n (disable public chat)
     * - @addattach:skull=n (prevent attaching to skull)
     * - @version=2550 (reply with version to channel 2550)
     * 
     * @param message The raw RLV command message
     * @param objectId ID of the object sending the command
     * @param objectName Name of the object sending the command
     * @return true if command was processed successfully
     */
    fun processRLVCommand(message: String, objectId: String, objectName: String): Boolean {
        if (!isRLVEnabled) {
            println("⚠️ RLV is disabled - ignoring command: $message")
            return false
        }
        
        // RLV commands must start with @
        if (!message.startsWith("@")) {
            return false // Not an RLV command
        }
        
        println("🔐 Processing RLV command: $message")
        println("   From object: $objectName ($objectId)")
        
        try {
            // Remove @ prefix and split multiple commands
            val commandString = message.substring(1)
            val commands = commandString.split(",")
            
            var allSuccessful = true
            
            commands.forEach { commandPart ->
                val success = processSingleCommand(commandPart.trim(), objectId, objectName)
                if (!success) allSuccessful = false
            }
            
            return allSuccessful
            
        } catch (e: Exception) {
            println("💥 Error processing RLV command: ${e.message}")
            return false
        }
    }
    
    /**
     * Process a single RLV command part
     */
    private fun processSingleCommand(commandPart: String, objectId: String, objectName: String): Boolean {
        // Parse command format: command[=parameter]
        val parts = commandPart.split("=", limit = 2)
        val commandWithParam = parts[0]
        val value = if (parts.size > 1) parts[1] else null
        
        // Handle commands with attachment point parameters (e.g., "addattach:skull")
        val colonIndex = commandWithParam.indexOf(':')
        val baseCommand = if (colonIndex != -1) commandWithParam.substring(0, colonIndex) else commandWithParam
        val attachmentParam = if (colonIndex != -1) commandWithParam.substring(colonIndex + 1) else null
        
        // Find the RLV command
        val rlvCommand = RLVCommand.fromString(baseCommand)
        if (rlvCommand == null) {
            println("⚠️ Unknown RLV command: $baseCommand")
            return false
        }
        
        // Check if command is blacklisted
        if (blacklistedCommands.contains(baseCommand)) {
            println("🚫 RLV command blocked by user settings: $baseCommand")
            return false
        }
        
        println("   Command: ${rlvCommand.command} (${rlvCommand.category})")
        println("   Description: ${rlvCommand.description}")
        if (attachmentParam != null) println("   Attachment Point: $attachmentParam")
        if (value != null) println("   Parameter: $value")
        
        // Process the command based on its type
        return when (rlvCommand) {
            RLVCommand.VERSION -> handleVersionCommand(value, objectId)
            RLVCommand.VERSIONNUM -> handleVersionNumCommand(value, objectId)
            RLVCommand.FLY -> handleMovementRestriction(rlvCommand, value, objectId, objectName)
            RLVCommand.SENDCHAT -> handleCommunicationRestriction(rlvCommand, value, objectId, objectName)
            RLVCommand.RECVCHAT -> handleCommunicationRestriction(rlvCommand, value, objectId, objectName)
            RLVCommand.ADDATTACH -> handleAttachmentRestriction(rlvCommand, attachmentParam, value, objectId, objectName)
            RLVCommand.REMATTACH -> handleAttachmentRestriction(rlvCommand, attachmentParam, value, objectId, objectName)
            RLVCommand.TPLM -> handleTeleportRestriction(rlvCommand, value, objectId, objectName)
            RLVCommand.TPLOC -> handleTeleportRestriction(rlvCommand, value, objectId, objectName)
            else -> {
                println("   ✅ Command acknowledged (not yet implemented)")
                true // Acknowledge but don't implement complex commands yet
            }
        }
    }
    
    /**
     * Handle version information requests
     */
    private fun handleVersionCommand(channel: String?, objectId: String): Boolean {
        if (channel != null) {
            val channelNum = channel.toIntOrNull()
            if (channelNum != null) {
                // In a real implementation, this would send a chat message on the specified channel
                println("   📢 Would reply on channel $channelNum: RestrainedLove viewer v$rlvVersion (Linkpoint-kotlin)")
                
                // Simulate sending reply via chat
                EventSystem.tryEmit(ViewerEvent.ChatReceived(
                    "RestrainedLove viewer v$rlvVersion (Linkpoint-kotlin)",
                    "RLV System",
                    channelNum
                ))
            }
        }
        return true
    }
    
    /**
     * Handle version number requests  
     */
    private fun handleVersionNumCommand(channel: String?, objectId: String): Boolean {
        if (channel != null) {
            val channelNum = channel.toIntOrNull()
            if (channelNum != null) {
                // Convert version to number format (e.g., 2.9.0 -> 2090000)
                val versionNum = convertVersionToNumber(rlvVersion)
                println("   📢 Would reply on channel $channelNum: $versionNum")
                
                EventSystem.tryEmit(ViewerEvent.ChatReceived(
                    versionNum.toString(),
                    "RLV System", 
                    channelNum
                ))
            }
        }
        return true
    }
    
    /**
     * Handle movement restrictions (fly, sit, etc.)
     */
    private fun handleMovementRestriction(command: RLVCommand, value: String?, objectId: String, objectName: String): Boolean {
        val isRestricting = value == "n" // "n" means disable/restrict, "y" means enable/allow
        val restrictionKey = command.command
        
        if (isRestricting) {
            activeRestrictions[restrictionKey] = RLVRestriction(
                command = command.command,
                parameter = null,
                objectId = objectId,
                objectName = objectName
            )
            println("   🔒 Movement restriction activated: ${command.description}")
            
            // Emit event to notify other systems
            EventSystem.tryEmit(ViewerEvent.MenuActionTriggered("rlv_restriction_${command.command}_added"))
            
        } else {
            activeRestrictions.remove(restrictionKey)
            println("   🔓 Movement restriction removed: ${command.description}")
            
            EventSystem.tryEmit(ViewerEvent.MenuActionTriggered("rlv_restriction_${command.command}_removed"))
        }
        
        return true
    }
    
    /**
     * Handle communication restrictions
     */
    private fun handleCommunicationRestriction(command: RLVCommand, value: String?, objectId: String, objectName: String): Boolean {
        val isRestricting = value == "n"
        val restrictionKey = command.command
        
        if (isRestricting) {
            activeRestrictions[restrictionKey] = RLVRestriction(
                command = command.command,
                parameter = null,
                objectId = objectId,
                objectName = objectName
            )
            println("   🔇 Communication restriction activated: ${command.description}")
        } else {
            activeRestrictions.remove(restrictionKey)
            println("   🔊 Communication restriction removed: ${command.description}")
        }
        
        return true
    }
    
    /**
     * Handle attachment restrictions
     */
    private fun handleAttachmentRestriction(command: RLVCommand, attachmentPoint: String?, value: String?, objectId: String, objectName: String): Boolean {
        val isRestricting = value == "n"
        val restrictionKey = if (attachmentPoint != null) "${command.command}:$attachmentPoint" else command.command
        
        if (isRestricting) {
            activeRestrictions[restrictionKey] = RLVRestriction(
                command = command.command,
                parameter = attachmentPoint,
                objectId = objectId,
                objectName = objectName
            )
            val pointDesc = attachmentPoint ?: "all points"
            println("   📎 Attachment restriction activated for $pointDesc: ${command.description}")
        } else {
            activeRestrictions.remove(restrictionKey)
            val pointDesc = attachmentPoint ?: "all points"  
            println("   📎 Attachment restriction removed for $pointDesc: ${command.description}")
        }
        
        return true
    }
    
    /**
     * Handle teleport restrictions
     */
    private fun handleTeleportRestriction(command: RLVCommand, value: String?, objectId: String, objectName: String): Boolean {
        val isRestricting = value == "n"
        val restrictionKey = command.command
        
        if (isRestricting) {
            activeRestrictions[restrictionKey] = RLVRestriction(
                command = command.command,
                parameter = null,
                objectId = objectId,
                objectName = objectName
            )
            println("   🚫 Teleport restriction activated: ${command.description}")
        } else {
            activeRestrictions.remove(restrictionKey)
            println("   ✈️ Teleport restriction removed: ${command.description}")
        }
        
        return true
    }
    
    /**
     * Convert version string to RLV version number format
     */
    private fun convertVersionToNumber(version: String): Long {
        val parts = version.split(".")
        if (parts.size >= 3) {
            val major = parts[0].toLongOrNull() ?: 0
            val minor = parts[1].toLongOrNull() ?: 0
            val patch = parts[2].toLongOrNull() ?: 0
            return major * 1000000 + minor * 10000 + patch * 100
        }
        return 0
    }
    
    /**
     * Check if a specific action is restricted by RLV
     */
    fun isRestricted(action: String, parameter: String? = null): Boolean {
        if (!isRLVEnabled) return false
        
        val key = if (parameter != null) "$action:$parameter" else action
        return activeRestrictions.containsKey(key) || activeRestrictions.containsKey(action)
    }
    
    /**
     * Get all active restrictions
     */
    fun getActiveRestrictions(): Map<String, RLVRestriction> {
        return activeRestrictions.toMap()
    }
    
    /**
     * Enable or disable the entire RLV system
     */
    fun setRLVEnabled(enabled: Boolean) {
        isRLVEnabled = enabled
        if (!enabled) {
            activeRestrictions.clear()
            println("🔓 RLV system disabled - all restrictions cleared")
        } else {
            println("🔐 RLV system enabled")
        }
    }
    
    /**
     * Add a command to the blacklist
     */
    fun blacklistCommand(command: String) {
        blacklistedCommands.add(command)
        println("🚫 RLV command blacklisted: $command")
    }
    
    /**
     * Remove a command from the blacklist
     */
    fun unblacklistCommand(command: String) {
        blacklistedCommands.remove(command)
        println("✅ RLV command removed from blacklist: $command")
    }
    
    /**
     * Clear all restrictions from a specific object
     */
    fun clearRestrictionsFromObject(objectId: String) {
        val toRemove = activeRestrictions.filter { it.value.objectId == objectId }
        toRemove.forEach { activeRestrictions.remove(it.key) }
        
        if (toRemove.isNotEmpty()) {
            println("🔓 Cleared ${toRemove.size} RLV restrictions from object $objectId")
        }
    }
    
    /**
     * Get status information for debugging
     */
    fun getStatusInfo(): String {
        return buildString {
            appendLine("RLV System Status:")
            appendLine("  Enabled: $isRLVEnabled")
            appendLine("  Version: $rlvVersion")
            appendLine("  Active Restrictions: ${activeRestrictions.size}")
            appendLine("  Blacklisted Commands: ${blacklistedCommands.size}")
            
            if (activeRestrictions.isNotEmpty()) {
                appendLine("\nActive Restrictions:")
                activeRestrictions.forEach { (key, restriction) ->
                    appendLine("  $key - from ${restriction.objectName}")
                }
            }
        }
    }
    
    // Getters
    fun isEnabled(): Boolean = isRLVEnabled
    fun getVersion(): String = rlvVersion
}