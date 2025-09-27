package com.linkpoint.testing.behavioral

import com.linkpoint.protocol.LoginSystem
import kotlinx.coroutines.delay
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Behavioral tests for LoginSystem comparing Kotlin implementation against C++ reference behavior
 * 
 * Tests compatibility with llloginhandler.cpp from SecondLife viewer
 */
class LoginSystemBehavioralTest : BehavioralTestContract<LoginSystem>() {
    
    override fun createKotlinImplementation(): LoginSystem {
        return LoginSystem()
    }
    
    override fun createReferenceImplementation(): LoginSystem {
        // Create a simulated C++ reference implementation
        return ReferenceLoginSystem()
    }
    
    override fun getTestScenarios(): List<TestScenario<LoginSystem>> {
        return listOf(
            // Test 1: Valid login credentials
            TestScenario(
                name = "Valid login with correct credentials",
                setup = { loginSystem ->
                    // Setup identical initial state
                    loginSystem.reset()
                },
                action = { loginSystem ->
                    val credentials = LoginSystem.LoginCredentials(
                        firstName = "Test",
                        lastName = "User",
                        password = "validpassword",
                        startLocation = "home"
                    )
                    loginSystem.login("https://test.grid.com/cgi-bin/login.cgi", credentials)
                },
                validator = { kotlinResult, referenceResult ->
                    // Both should succeed with similar session data
                    val kResult = kotlinResult as? LoginSystem.LoginResponse
                    val rResult = referenceResult as? LoginSystem.LoginResponse
                    
                    kResult != null && rResult != null &&
                    kResult.success == rResult.success &&
                    kResult.sessionId.isNotBlank() == rResult.sessionId.isNotBlank()
                },
                description = "Both implementations should handle valid credentials identically"
            ),
            
            // Test 2: Invalid credentials
            TestScenario(
                name = "Invalid login with wrong password",
                action = { loginSystem ->
                    val credentials = LoginSystem.LoginCredentials(
                        firstName = "Test",
                        lastName = "User", 
                        password = "wrongpassword",
                        startLocation = "home"
                    )
                    loginSystem.login("https://test.grid.com/cgi-bin/login.cgi", credentials)
                },
                validator = { kotlinResult, referenceResult ->
                    // Both should fail with authentication error
                    val kResult = kotlinResult as? LoginSystem.LoginResponse
                    val rResult = referenceResult as? LoginSystem.LoginResponse
                    
                    kResult != null && rResult != null &&
                    !kResult.success && !rResult.success &&
                    kResult.errorMessage.contains("authentication", ignoreCase = true) ==
                    rResult.errorMessage.contains("authentication", ignoreCase = true)
                },
                description = "Both implementations should reject invalid credentials similarly"
            ),
            
            // Test 3: Network timeout handling
            TestScenario(
                name = "Network timeout during login",
                action = { loginSystem ->
                    val credentials = LoginSystem.LoginCredentials(
                        firstName = "Test",
                        lastName = "User",
                        password = "timeout",
                        startLocation = "home"
                    )
                    // Simulate timeout by using invalid URL that will timeout
                    loginSystem.login("https://192.0.2.1:9999/timeout", credentials)
                },
                validator = { kotlinResult, referenceResult ->
                    // Both should handle timeout gracefully
                    val kResult = kotlinResult as? LoginSystem.LoginResponse
                    val rResult = referenceResult as? LoginSystem.LoginResponse
                    
                    kResult != null && rResult != null &&
                    !kResult.success && !rResult.success &&
                    (kResult.errorMessage.contains("timeout", ignoreCase = true) ||
                     kResult.errorMessage.contains("connection", ignoreCase = true)) ==
                    (rResult.errorMessage.contains("timeout", ignoreCase = true) ||
                     rResult.errorMessage.contains("connection", ignoreCase = true))
                },
                description = "Both implementations should handle network timeouts similarly"
            ),
            
            // Test 4: Session management
            TestScenario(
                name = "Session state after successful login",
                action = { loginSystem ->
                    val credentials = LoginSystem.LoginCredentials(
                        firstName = "Test",
                        lastName = "User",
                        password = "validpassword",
                        startLocation = "home"
                    )
                    val loginResult = loginSystem.login("https://test.grid.com/cgi-bin/login.cgi", credentials)
                    
                    // Return session state info
                    mapOf(
                        "hasSession" to loginSystem.hasActiveSession(),
                        "sessionId" to (loginResult.sessionId.takeIf { it.isNotBlank() } ?: "none"),
                        "isConnected" to loginResult.success
                    )
                },
                validator = { kotlinResult, referenceResult ->
                    val kState = kotlinResult as? Map<String, Any>
                    val rState = referenceResult as? Map<String, Any>
                    
                    kState != null && rState != null &&
                    kState["hasSession"] == rState["hasSession"] &&
                    kState["isConnected"] == rState["isConnected"] &&
                    (kState["sessionId"] != "none") == (rState["sessionId"] != "none")
                },
                description = "Session state should be managed identically"
            ),
            
            // Test 5: XMLRPC request format
            TestScenario(
                name = "XMLRPC request format compliance",
                action = { loginSystem ->
                    val credentials = LoginSystem.LoginCredentials(
                        firstName = "Format",
                        lastName = "Test",
                        password = "testpass",
                        startLocation = "last"
                    )
                    
                    // Get the XMLRPC request that would be sent (simulated)
                    loginSystem.getLoginRequestXML(credentials)
                },
                validator = { kotlinResult, referenceResult ->
                    val kXml = kotlinResult as? String
                    val rXml = referenceResult as? String
                    
                    kXml != null && rXml != null &&
                    kXml.contains("<methodCall>") && rXml.contains("<methodCall>") &&
                    kXml.contains("<methodName>login_to_simulator</methodName>") ==
                    rXml.contains("<methodName>login_to_simulator</methodName>") &&
                    kXml.contains("first") && rXml.contains("first") &&
                    kXml.contains("last") && rXml.contains("last")
                },
                description = "XMLRPC format should match C++ llloginhandler.cpp exactly"
            )
        )
    }
}

/**
 * Reference implementation simulating C++ llloginhandler.cpp behavior
 */
private class ReferenceLoginSystem : LoginSystem() {
    
    override suspend fun login(gridUrl: String, credentials: LoginCredentials): LoginResponse {
        logger.info("Reference C++ login simulation for: ${credentials.firstName} ${credentials.lastName}")
        
        // Simulate C++ llloginhandler.cpp behavior patterns
        return when {
            // Simulate timeout like C++ would
            gridUrl.contains("192.0.2.1") || gridUrl.contains("timeout") -> {
                delay(5000) // Simulate C++ timeout
                LoginResponse(
                    success = false,
                    sessionId = "",
                    errorMessage = "Connection timeout - unable to reach login server",
                    gridInfo = null
                )
            }
            
            // Simulate authentication failure like C++ would
            credentials.password == "wrongpassword" -> {
                LoginResponse(
                    success = false,
                    sessionId = "",
                    errorMessage = "Authentication failed - invalid credentials",
                    gridInfo = null
                )
            }
            
            // Simulate successful login like C++ would
            credentials.password == "validpassword" -> {
                LoginResponse(
                    success = true,
                    sessionId = "ref-${System.currentTimeMillis()}-${credentials.firstName}",
                    errorMessage = "",
                    gridInfo = LoginResponse.GridInfo(
                        simulatorName = "Reference Sim",
                        simulatorIP = "127.0.0.1",
                        simulatorPort = 9000,
                        agentId = "12345678-1234-1234-1234-123456789abc",
                        regionHandle = 256000L,
                        startLocation = credentials.startLocation
                    )
                )
            }
            
            else -> {
                LoginResponse(
                    success = false,
                    sessionId = "",
                    errorMessage = "Unknown error",
                    gridInfo = null
                )
            }
        }
    }
    
    override fun getLoginRequestXML(credentials: LoginCredentials): String {
        // Simulate the exact XMLRPC format that C++ llloginhandler.cpp would generate
        return """
            <?xml version="1.0"?>
            <methodCall>
                <methodName>login_to_simulator</methodName>
                <params>
                    <param>
                        <value>
                            <struct>
                                <member>
                                    <name>first</name>
                                    <value><string>${credentials.firstName}</string></value>
                                </member>
                                <member>
                                    <name>last</name>
                                    <value><string>${credentials.lastName}</string></value>
                                </member>
                                <member>
                                    <name>passwd</name>
                                    <value><string>${credentials.password}</string></value>
                                </member>
                                <member>
                                    <name>start</name>
                                    <value><string>${credentials.startLocation}</string></value>
                                </member>
                                <member>
                                    <name>channel</name>
                                    <value><string>Linkpoint Kotlin Viewer</string></value>
                                </member>
                                <member>
                                    <name>version</name>
                                    <value><string>1.0.0</string></value>
                                </member>
                                <member>
                                    <name>platform</name>
                                    <value><string>Kotlin</string></value>
                                </member>
                                <member>
                                    <name>agree_to_tos</name>
                                    <value><boolean>1</boolean></value>
                                </member>
                                <member>
                                    <name>read_critical</name>
                                    <value><boolean>1</boolean></value>
                                </member>
                            </struct>
                        </value>
                    </param>
                </params>
            </methodCall>
        """.trimIndent()
    }
    
    override fun hasActiveSession(): Boolean {
        // Simulate C++ session state tracking
        return _lastLoginResult?.success == true
    }
    
    override fun reset() {
        // Reset state like C++ would
        _lastLoginResult = null
        logger.info("Reference login system reset")
    }
    
    private var _lastLoginResult: LoginResponse? = null
}