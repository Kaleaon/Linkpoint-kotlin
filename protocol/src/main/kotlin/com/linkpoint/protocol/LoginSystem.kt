package com.linkpoint.protocol

import com.linkpoint.core.events.EventSystem
import com.linkpoint.core.events.ViewerEvent
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URL
import java.net.HttpURLConnection
import java.io.OutputStreamWriter
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * XMLRPC Response data classes for parsing SecondLife login responses
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class XmlRpcMethodResponse(
    @JsonProperty("params") val params: XmlRpcParams? = null,
    @JsonProperty("fault") val fault: XmlRpcFault? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class XmlRpcParams(
    @JsonProperty("param") val param: XmlRpcParam
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class XmlRpcParam(
    @JsonProperty("value") val value: XmlRpcValue
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class XmlRpcValue(
    @JsonProperty("struct") val struct: XmlRpcStruct? = null,
    @JsonProperty("string") val string: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class XmlRpcStruct(
    @JsonProperty("member") val members: List<XmlRpcMember>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class XmlRpcMember(
    @JsonProperty("name") val name: String,
    @JsonProperty("value") val value: XmlRpcMemberValue
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class XmlRpcMemberValue(
    @JsonProperty("string") val string: String? = null,
    @JsonProperty("int") val int: String? = null,
    @JsonProperty("array") val array: XmlRpcArray? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class XmlRpcArray(
    @JsonProperty("data") val data: XmlRpcArrayData
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class XmlRpcArrayData(
    @JsonProperty("value") val values: List<XmlRpcValue>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class XmlRpcFault(
    @JsonProperty("value") val value: XmlRpcValue
)

/**
 * XMLRPC Login System for SecondLife/OpenSim compatible grids.
 * 
 * This class implements the login authentication process used by virtual world viewers.
 * It's imported and modernized from the SecondLife viewer's login handling:
 * - Original C++: llloginhandler.cpp, lllogininstance.cpp
 * - Firestorm enhancements: Advanced login options and grid management
 * - Modern Kotlin implementation: Coroutines, null safety, and proper error handling
 * 
 * The login process follows these steps:
 * 1. Prepare XMLRPC login request with user credentials
 * 2. Send request to grid's login endpoint
 * 3. Parse response containing session info and simulator details
 * 4. Extract session ID, agent ID, and initial simulator connection info
 * 5. Emit login success/failure events for other systems
 */
class LoginSystem {
    
    // Login state tracking
    private var isLoggedIn = false
    private var currentSessionId: String? = null
    private var currentAgentId: String? = null
    private var currentSimulatorHost: String? = null
    private var currentSimulatorPort: Int = 0
    
    /**
     * Data class representing login credentials
     * Mirrors the structure used in SecondLife viewer login forms
     */
    data class LoginCredentials(
        val firstName: String,
        val lastName: String,
        val password: String,
        val startLocation: String = "home", // Default to user's home location
        val channel: String = "Linkpoint-kotlin", // Viewer identification
        val version: String = "0.1.0"
    )
    
    /**
     * Data class representing successful login response
     * Based on SecondLife XMLRPC login response format
     */
    data class LoginResponse(
        val success: Boolean,
        val sessionId: String?,
        val agentId: String?,
        val secureSessionId: String?,
        val simIp: String?,
        val simPort: Int,
        val seedCapability: String?, // Capability URL for HTTP services
        val circuitCode: Int,
        val lookAt: List<Float>?, // Initial camera look direction
        val agentAccess: String?, // Access level (e.g., "M" for Mature)
        val message: String? = null, // Error message if login failed
        val reason: String? = null   // Detailed error reason
    )
    
    /**
     * Attempt to log into a SecondLife/OpenSim compatible grid
     * 
     * This method implements the XMLRPC login protocol used by all SecondLife-compatible viewers.
     * The protocol is documented in the OpenMetaverse library and SecondLife viewer source.
     * 
     * @param loginUri The grid's login endpoint (e.g., "https://login.agni.lindenlab.com/cgi-bin/login.cgi")
     * @param credentials User login credentials
     * @return LoginResponse containing session info or error details
     */
    suspend fun login(loginUri: String, credentials: LoginCredentials): LoginResponse {
        println("🔐 Starting login process to: $loginUri")
        println("   User: ${credentials.firstName} ${credentials.lastName}")
        println("   Start Location: ${credentials.startLocation}")
        
        try {
            // Step 1: Build XMLRPC login request
            // This follows the exact format expected by SecondLife login servers
            val xmlRequest = buildLoginXMLRequest(credentials)
            println("📤 Sending XMLRPC login request...")
            
            // Step 2: Send HTTP POST request to login endpoint
            val xmlResponse = sendXMLRPCRequest(loginUri, xmlRequest)
            println("📥 Received login response")
            
            // Step 3: Parse XMLRPC response
            val loginResponse = parseLoginResponse(xmlResponse)
            
            if (loginResponse.success) {
                // Step 4: Store session information
                isLoggedIn = true
                currentSessionId = loginResponse.sessionId
                currentAgentId = loginResponse.agentId
                currentSimulatorHost = loginResponse.simIp
                currentSimulatorPort = loginResponse.simPort
                
                println("✅ Login successful!")
                println("   Session ID: ${loginResponse.sessionId}")
                println("   Agent ID: ${loginResponse.agentId}")
                println("   Simulator: ${loginResponse.simIp}:${loginResponse.simPort}")
                
                // Step 5: Notify other systems of successful login
                EventSystem.tryEmit(ViewerEvent.Connected(loginResponse.sessionId ?: "unknown"))
                
            } else {
                println("❌ Login failed: ${loginResponse.message}")
                println("   Reason: ${loginResponse.reason}")
                
                // Notify other systems of login failure
                EventSystem.tryEmit(ViewerEvent.ConnectionFailed(
                    loginResponse.message ?: "Unknown login error"
                ))
            }
            
            return loginResponse
            
        } catch (e: Exception) {
            println("💥 Login error: ${e.message}")
            val errorResponse = LoginResponse(
                success = false,
                sessionId = null,
                agentId = null,
                secureSessionId = null,
                simIp = null,
                simPort = 0,
                seedCapability = null,
                circuitCode = 0,
                lookAt = null,
                agentAccess = null,
                message = "Connection error: ${e.message}",
                reason = e.javaClass.simpleName
            )
            
            EventSystem.tryEmit(ViewerEvent.ConnectionFailed(e.message ?: "Login exception"))
            return errorResponse
        }
    }
    
    /**
     * Build XMLRPC login request following SecondLife protocol specification
     * 
     * The request format is standardized and must include specific fields
     * that the login server expects. This is based on the login request
     * format from the original SecondLife viewer.
     */
    private fun buildLoginXMLRequest(credentials: LoginCredentials): String {
        return """<?xml version="1.0"?>
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
                        <value><string>${credentials.channel}</string></value>
                    </member>
                    <member>
                        <name>version</name>
                        <value><string>${credentials.version}</string></value>
                    </member>
                    <member>
                        <name>platform</name>
                        <value><string>Kotlin</string></value>
                    </member>
                    <member>
                        <name>mac</name>
                        <value><string>false</string></value>
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
</methodCall>"""
    }
    
    /**
     * Send XMLRPC request using HTTP POST
     * This implements the HTTP transport layer for XMLRPC communication
     */
    private fun sendXMLRPCRequest(loginUri: String, xmlRequest: String): String {
        val url = URL(loginUri)
        val connection = url.openConnection() as HttpURLConnection
        
        // Configure HTTP request for XMLRPC
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "text/xml")
        connection.setRequestProperty("User-Agent", "Linkpoint-kotlin/0.1.0")
        connection.doOutput = true
        connection.connectTimeout = 30000 // 30 second timeout
        connection.readTimeout = 30000
        
        // Send XML request
        val writer = OutputStreamWriter(connection.outputStream)
        writer.write(xmlRequest)
        writer.flush()
        writer.close()
        
        // Read XML response
        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val response = reader.readText()
        reader.close()
        
        return response
    }
    
    /**
     * Parse XMLRPC login response into LoginResponse data class
     * 
     * Parses the XML response from SecondLife/OpenSim login servers.
     * Implements proper XML parsing using Jackson XML mapper.
     */
    private fun parseLoginResponse(xmlResponse: String): LoginResponse {
        println("📋 Parsing login response...")
        
        try {
            val xmlMapper = XmlMapper().registerKotlinModule()
            val response = xmlMapper.readValue(xmlResponse, XmlRpcMethodResponse::class.java)
            
            // Check for fault responses
            if (response.fault != null) {
                return LoginResponse(
                    success = false,
                    sessionId = null,
                    agentId = null,
                    secureSessionId = null,
                    simIp = null,
                    simPort = 0,
                    seedCapability = null,
                    circuitCode = 0,
                    lookAt = null,
                    agentAccess = null,
                    message = "Login failed - server returned fault",
                    reason = "Server fault"
                )
            }
            
            // Parse successful response
            val struct = response.params?.param?.value?.struct
            if (struct == null) {
                return LoginResponse(
                    success = false,
                    sessionId = null,
                    agentId = null,
                    secureSessionId = null,
                    simIp = null,
                    simPort = 0,
                    seedCapability = null,
                    circuitCode = 0,
                    lookAt = null,
                    agentAccess = null,
                    message = "Login failed - invalid response format",
                    reason = "Invalid response"
                )
            }
            
            // Extract values from response
            val memberMap = struct.members.associate { it.name to it.value }
            
            val sessionId = memberMap["session_id"]?.string
            val agentId = memberMap["agent_id"]?.string
            val secureSessionId = memberMap["secure_session_id"]?.string
            val simIp = memberMap["sim_ip"]?.string
            val simPort = memberMap["sim_port"]?.int?.toIntOrNull() ?: 0
            val seedCapability = memberMap["seed_capability"]?.string
            val circuitCode = memberMap["circuit_code"]?.int?.toIntOrNull() ?: 0
            val agentAccess = memberMap["agent_access"]?.string
            val message = memberMap["message"]?.string ?: "Login successful"
            
            // Parse look_at array if present
            val lookAtArray = memberMap["look_at"]?.array?.data?.values
            val lookAt = lookAtArray?.mapNotNull { it.string?.toFloatOrNull() }
            
            // Check if we have minimum required fields
            if (sessionId == null || agentId == null) {
                return LoginResponse(
                    success = false,
                    sessionId = null,
                    agentId = null,
                    secureSessionId = null,
                    simIp = null,
                    simPort = 0,
                    seedCapability = null,
                    circuitCode = 0,
                    lookAt = null,
                    agentAccess = null,
                    message = "Login failed - missing required fields",
                    reason = "Missing session or agent ID"
                )
            }
            
            return LoginResponse(
                success = true,
                sessionId = sessionId,
                agentId = agentId,
                secureSessionId = secureSessionId,
                simIp = simIp,
                simPort = simPort,
                seedCapability = seedCapability,
                circuitCode = circuitCode,
                lookAt = lookAt,
                agentAccess = agentAccess,
                message = message,
                reason = null
            )
            
        } catch (e: Exception) {
            println("❌ Error parsing XML response: ${e.message}")
            return LoginResponse(
                success = false,
                sessionId = null,
                agentId = null,
                secureSessionId = null,
                simIp = null,
                simPort = 0,
                seedCapability = null,
                circuitCode = 0,
                lookAt = null,
                agentAccess = null,
                message = "Login failed - XML parsing error: ${e.message}",
                reason = "XML parsing error"
            )
        }
    }
    
    /**
     * Log out from the current session
     * Implements graceful logout following SecondLife viewer patterns
     */
    suspend fun logout() {
        if (!isLoggedIn) {
            println("⚠️ No active session to logout from")
            return
        }
        
        println("🚪 Logging out from session: $currentSessionId")
        
        try {
            // In a full implementation, this would:
            // 1. Send logout message to simulator
            // 2. Close UDP connection
            // 3. Clean up session resources
            
            // Notify other systems of disconnect
            EventSystem.tryEmit(ViewerEvent.Disconnected("User logout"))
            
            // Clear session state
            isLoggedIn = false
            currentSessionId = null
            currentAgentId = null
            currentSimulatorHost = null
            currentSimulatorPort = 0
            
            println("✅ Logout complete")
            
        } catch (e: Exception) {
            println("⚠️ Error during logout: ${e.message}")
        }
    }
    
    // Getters for session information
    fun isLoggedIn(): Boolean = isLoggedIn
    fun getSessionId(): String? = currentSessionId
    fun getAgentId(): String? = currentAgentId
    fun getSimulatorEndpoint(): Pair<String?, Int> = currentSimulatorHost to currentSimulatorPort
}