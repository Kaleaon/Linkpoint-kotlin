package com.linkpoint.ui

/**
 * Login Dialog for SecondLife/OpenSim Grid Authentication
 * 
 * This implements a simple login interface for virtual world connectivity.
 * Inspired by SecondLife viewer and Firestorm login screens.
 */

/**
 * Login credentials data class
 */
data class LoginCredentials(
    val firstName: String,
    val lastName: String,
    val password: String,
    val gridUrl: String = "https://login.agni.lindenlab.com/cgi-bin/login.cgi",
    val startLocation: String = "home"
)

/**
 * Grid configuration for different virtual worlds
 */
data class GridConfig(
    val name: String,
    val loginUrl: String,
    val isSecondLife: Boolean = false
) {
    companion object {
        val SECOND_LIFE_MAIN = GridConfig(
            "Second Life (Main Grid)", 
            "https://login.agni.lindenlab.com/cgi-bin/login.cgi",
            true
        )
        
        val SECOND_LIFE_BETA = GridConfig(
            "Second Life (Beta Grid)", 
            "https://login.aditi.lindenlab.com/cgi-bin/login.cgi",
            true
        )
        
        val OPENSIM_LOCAL = GridConfig(
            "OpenSim (Local)", 
            "http://127.0.0.1:8002/",
            false
        )
        
        fun getDefaultGrids(): List<GridConfig> = listOf(
            SECOND_LIFE_MAIN,
            SECOND_LIFE_BETA,
            OPENSIM_LOCAL
        )
    }
}

/**
 * Simple console-based login dialog
 * TODO: Replace with proper GUI implementation
 */
class LoginDialog {
    
    private val availableGrids = GridConfig.getDefaultGrids()
    
    /**
     * Show login dialog and collect credentials
     */
    fun showLoginDialog(): LoginCredentials? {
        println("═══════════════════════════════════════════════")
        println("    Linkpoint-kotlin Virtual World Login")
        println("═══════════════════════════════════════════════")
        println()
        
        try {
            // Grid selection
            println("Available Grids:")
            availableGrids.forEachIndexed { index, grid ->
                println("  ${index + 1}. ${grid.name}")
            }
            println()
            
            print("Select grid (1-${availableGrids.size}): ")
            val gridChoice = readLine()?.toIntOrNull()
            if (gridChoice == null || gridChoice !in 1..availableGrids.size) {
                println("❌ Invalid grid selection")
                return null
            }
            
            val selectedGrid = availableGrids[gridChoice - 1]
            println("Selected: ${selectedGrid.name}")
            println()
            
            // Credential collection
            print("First Name: ")
            val firstName = readLine()?.trim()
            if (firstName.isNullOrBlank()) {
                println("❌ First name is required")
                return null
            }
            
            print("Last Name: ")
            val lastName = readLine()?.trim()  
            if (lastName.isNullOrBlank()) {
                println("❌ Last name is required")
                return null
            }
            
            print("Password: ")
            val password = readLine()?.trim()
            if (password.isNullOrBlank()) {
                println("❌ Password is required")
                return null
            }
            
            // Start location (optional)
            print("Start Location (home/last/region name) [home]: ")
            val startLocation = readLine()?.trim()?.takeIf { it.isNotBlank() } ?: "home"
            
            println()
            println("✅ Login credentials collected")
            println("   Grid: ${selectedGrid.name}")
            println("   User: $firstName $lastName")
            println("   Start: $startLocation")
            println()
            
            return LoginCredentials(
                firstName = firstName,
                lastName = lastName,
                password = password,
                gridUrl = selectedGrid.loginUrl,
                startLocation = startLocation
            )
            
        } catch (e: Exception) {
            println("❌ Error in login dialog: ${e.message}")
            return null
        }
    }
    
    /**
     * Show simple login progress
     */
    fun showLoginProgress(status: String) {
        println("🔄 $status")
    }
    
    /**
     * Show login success
     */
    fun showLoginSuccess(gridName: String, region: String?) {
        println("✅ Login Successful!")
        println("   Grid: $gridName")
        if (region != null) {
            println("   Region: $region")
        }
        println()
    }
    
    /**
     * Show login failure
     */
    fun showLoginFailure(error: String) {
        println("❌ Login Failed!")
        println("   Error: $error")
        println()
    }
    
    /**
     * Show main menu after login
     */
    fun showMainMenu(): String? {
        println("═══════════════════════════════════════════════")
        println("    Main Menu")
        println("═══════════════════════════════════════════════")
        println("1. Stay Connected (basic session)")
        println("2. Send Test Chat Message")
        println("3. Disconnect and Exit")
        println()
        
        print("Select option (1-3): ")
        return readLine()?.trim()
    }
}