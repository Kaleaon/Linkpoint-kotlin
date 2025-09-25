package com.linkpoint

import com.linkpoint.core.SimpleViewerCore
import com.linkpoint.protocol.LoginSystem
import com.linkpoint.protocol.UDPMessageSystem
import com.linkpoint.ui.LoginDialog
import kotlinx.coroutines.*

/**
 * SecondLife-ready main application
 * 
 * This demonstrates actual SecondLife connectivity using the implemented
 * XML parsing and UDP message processing systems.
 */
suspend fun main(args: Array<String>) = coroutineScope {
    println("═".repeat(80))
    println("Linkpoint Kotlin - SecondLife Connectivity Test")
    println("Version: 0.1.0 - Phase 1 Implementation")
    println("═".repeat(80))
    println()
    
    val viewerCore = SimpleViewerCore()
    val loginSystem = LoginSystem()
    val udpSystem = UDPMessageSystem()
    val loginDialog = LoginDialog()
    
    try {
        // Initialize core systems
        println("🔧 Initializing core systems...")
        if (!viewerCore.initialize()) {
            println("❌ Failed to initialize viewer core")
            return@coroutineScope
        }
        
        if (!viewerCore.start()) {
            println("❌ Failed to start viewer")
            return@coroutineScope
        }
        
        println("✅ Core systems ready")
        println()
        
        // Show login dialog
        val credentials = loginDialog.showLoginDialog()
        if (credentials == null) {
            println("❌ Login cancelled or invalid credentials")
            return@coroutineScope
        }
        
        // Attempt login to SecondLife/OpenSim
        println("🌐 Attempting login to virtual world...")
        loginDialog.showLoginProgress("Connecting to grid...")
        
        val loginResponse = loginSystem.login(credentials.gridUrl, 
            LoginSystem.LoginCredentials(
                firstName = credentials.firstName,
                lastName = credentials.lastName,
                password = credentials.password,
                startLocation = credentials.startLocation,
                channel = "Linkpoint-kotlin",
                version = "0.1.0"
            )
        )
        
        if (!loginResponse.success) {
            loginDialog.showLoginFailure(loginResponse.message ?: "Unknown error")
            return@coroutineScope
        }
        
        loginDialog.showLoginSuccess("SecondLife/OpenSim", loginResponse.simIp)
        
        // Connect to simulator if login succeeded
        if (loginResponse.simIp != null && loginResponse.simPort > 0 && loginResponse.circuitCode > 0) {
            println("🔌 Connecting to simulator...")
            loginDialog.showLoginProgress("Establishing UDP connection...")
            
            val udpConnected = udpSystem.connect(
                loginResponse.simIp,
                loginResponse.simPort,
                loginResponse.circuitCode
            )
            
            if (udpConnected) {
                println("✅ Connected to virtual world!")
                println("   Session ID: ${loginResponse.sessionId}")
                println("   Agent ID: ${loginResponse.agentId}")
                println("   Simulator: ${loginResponse.simIp}:${loginResponse.simPort}")
                println()
                
                // Main loop
                var running = true
                while (running) {
                    val choice = loginDialog.showMainMenu()
                    
                    when (choice) {
                        "1" -> {
                            println("📡 Maintaining connection...")
                            println("   UDP message processing is active")
                            println("   Press Enter to return to menu")
                            readLine()
                        }
                        
                        "2" -> {
                            print("Enter chat message: ")
                            val chatMessage = readLine()?.trim()
                            if (!chatMessage.isNullOrBlank()) {
                                println("💬 Sending chat: \"$chatMessage\"")
                                udpSystem.sendChatMessage(chatMessage, 0)
                            }
                        }
                        
                        "3" -> {
                            println("🚪 Disconnecting...")
                            running = false
                        }
                        
                        else -> {
                            println("❌ Invalid option")
                        }
                    }
                }
                
                // Disconnect
                udpSystem.disconnect()
                loginSystem.logout()
                
            } else {
                println("❌ Failed to connect to simulator")
            }
        } else {
            println("⚠️ Login succeeded but no simulator connection info available")
        }
        
    } catch (e: Exception) {
        println("💥 Error: ${e.message}")
        e.printStackTrace()
    } finally {
        // Cleanup
        try {
            udpSystem.disconnect()
            loginSystem.logout()
            viewerCore.shutdown()
        } catch (e: Exception) {
            println("⚠️ Cleanup error: ${e.message}")
        }
        
        println()
        println("👋 SecondLife connectivity test complete")
        println("═".repeat(80))
    }
}