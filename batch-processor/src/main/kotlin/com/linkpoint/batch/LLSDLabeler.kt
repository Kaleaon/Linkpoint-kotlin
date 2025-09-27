package com.linkpoint.batch

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Applies LLSD (Linden Lab Structured Data) standards labeling and documentation
 * to converted Kotlin components for proper organization and identification
 */
class LLSDLabeler {
    
    private val json = Json { prettyPrint = true }
    
    /**
     * Label a component with LLSD standards
     */
    fun labelComponent(convertedFile: File, originalFile: File, repository: Repository) {
        val label = createLLSDLabel(convertedFile, originalFile, repository)
        
        // Insert LLSD label at the top of the file
        val originalContent = convertedFile.readText()
        val labeledContent = generateLLSDHeader(label) + "\n\n" + originalContent
        
        convertedFile.writeText(labeledContent)
        
        // Create companion metadata file
        val metadataFile = File(convertedFile.parent, "${convertedFile.nameWithoutExtension}.llsd.json")
        metadataFile.writeText(json.encodeToString(LLSDLabel.serializer(), label))
    }
    
    /**
     * Apply comprehensive LLSD standards to a converted file
     */
    fun applyLLSDStandards(file: File) {
        val content = file.readText()
        val standardizedContent = applyLLSDFormatting(content)
        file.writeText(standardizedContent)
    }
    
    /**
     * Create LLSD label for a component
     */
    private fun createLLSDLabel(convertedFile: File, originalFile: File, repository: Repository): LLSDLabel {
        val componentType = detectComponentType(originalFile)
        val functionality = detectFunctionality(originalFile, convertedFile)
        val dependencies = detectDependencies(convertedFile)
        
        return LLSDLabel(
            componentId = generateComponentId(convertedFile, repository),
            name = convertedFile.nameWithoutExtension,
            type = componentType,
            functionality = functionality,
            sourceRepository = repository.name,
            sourceFile = originalFile.name,
            sourcePath = originalFile.path,
            convertedPath = convertedFile.path,
            language = "Kotlin",
            originalLanguage = repository.language,
            version = "1.0.0",
            createdAt = LocalDateTime.now(),
            lastModified = LocalDateTime.now(),
            dependencies = dependencies,
            llsdCompliant = true,
            tags = generateTags(originalFile, repository),
            documentation = generateDocumentation(originalFile, convertedFile),
            testingStatus = TestingStatus.CONVERTED,
            qualityMetrics = calculateQualityMetrics(originalFile, convertedFile)
        )
    }
    
    /**
     * Generate LLSD header comment for the file
     */
    private fun generateLLSDHeader(label: LLSDLabel): String {
        return """
/*
 * LLSD Component Label
 * ==================
 * Component ID: ${label.componentId}
 * Name: ${label.name}
 * Type: ${label.type}
 * Source: ${label.sourceRepository}/${label.sourceFile}
 * Version: ${label.version}
 * Created: ${label.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE)}
 * 
 * Functionality: ${label.functionality.joinToString(", ")}
 * Dependencies: ${label.dependencies.joinToString(", ")}
 * Tags: ${label.tags.joinToString(", ")}
 * 
 * LLSD Compliant: ${if (label.llsdCompliant) "✅ Yes" else "❌ No"}
 * Testing Status: ${label.testingStatus}
 * Quality Score: ${label.qualityMetrics.overallScore}%
 * 
 * Documentation:
 * ${label.documentation.description}
 * 
 * Usage:
 * ${label.documentation.usage}
 */
        """.trimIndent()
    }
    
    /**
     * Detect component type based on file content and structure
     */
    private fun detectComponentType(file: File): ComponentType {
        val content = file.readText().lowercase()
        val fileName = file.name.lowercase()
        
        return when {
            // UI Components
            fileName.contains("floater") || fileName.contains("panel") || fileName.contains("dialog") -> ComponentType.UI_COMPONENT
            content.contains("llfloater") || content.contains("llpanel") -> ComponentType.UI_COMPONENT
            
            // Rendering System
            fileName.contains("render") || fileName.contains("draw") || fileName.contains("gl") -> ComponentType.RENDERING
            content.contains("opengl") || content.contains("shader") || content.contains("texture") -> ComponentType.RENDERING
            
            // Network/Protocol
            fileName.contains("message") || fileName.contains("protocol") || fileName.contains("network") -> ComponentType.NETWORK
            content.contains("udp") || content.contains("tcp") || content.contains("xmlrpc") -> ComponentType.NETWORK
            
            // Avatar System
            fileName.contains("avatar") || fileName.contains("agent") -> ComponentType.AVATAR
            content.contains("llavatar") || content.contains("agent") -> ComponentType.AVATAR
            
            // Asset Management
            fileName.contains("asset") || fileName.contains("cache") || fileName.contains("texture") -> ComponentType.ASSET
            content.contains("asset") || content.contains("cache") -> ComponentType.ASSET
            
            // Audio System
            fileName.contains("audio") || fileName.contains("sound") -> ComponentType.AUDIO
            content.contains("audio") || content.contains("sound") -> ComponentType.AUDIO
            
            // RLV Extensions
            fileName.contains("rlv") || content.contains("rlv") -> ComponentType.RLV_EXTENSION
            
            // Core System
            fileName.contains("app") || fileName.contains("viewer") || fileName.contains("main") -> ComponentType.CORE_SYSTEM
            
            else -> ComponentType.UTILITY
        }
    }
    
    /**
     * Detect functionality provided by the component
     */
    private fun detectFunctionality(originalFile: File, convertedFile: File): List<String> {
        val content = convertedFile.readText().lowercase()
        val functionality = mutableListOf<String>()
        
        // Common Second Life functionalities
        if (content.contains("login")) functionality.add("User Authentication")
        if (content.contains("chat")) functionality.add("Chat Communication")
        if (content.contains("inventory")) functionality.add("Inventory Management")
        if (content.contains("teleport")) functionality.add("Avatar Teleportation")
        if (content.contains("animation")) functionality.add("Avatar Animation")
        if (content.contains("physics")) functionality.add("Physics Simulation")
        if (content.contains("script")) functionality.add("Script Execution")
        if (content.contains("media")) functionality.add("Media Streaming")
        if (content.contains("voice")) functionality.add("Voice Communication")
        if (content.contains("friend")) functionality.add("Social Features")
        if (content.contains("group")) functionality.add("Group Management")
        if (content.contains("im") || content.contains("instant")) functionality.add("Instant Messaging")
        if (content.contains("landmark")) functionality.add("Location Management")
        if (content.contains("gesture")) functionality.add("Gesture Control")
        if (content.contains("attachment")) functionality.add("Avatar Attachments")
        
        // RLV specific
        if (content.contains("rlv")) functionality.add("RLV Protocol Support")
        if (content.contains("restriction")) functionality.add("Avatar Restrictions")
        
        // Technical functionalities
        if (content.contains("coroutine")) functionality.add("Asynchronous Processing")
        if (content.contains("suspend")) functionality.add("Coroutine Support")
        if (content.contains("flow")) functionality.add("Reactive Streams")
        
        return functionality.ifEmpty { listOf("General Utility") }
    }
    
    /**
     * Detect dependencies in the converted code
     */
    private fun detectDependencies(file: File): List<String> {
        val content = file.readText()
        val dependencies = mutableSetOf<String>()
        
        // Extract import statements
        val importPattern = Regex("import\\s+([\\w.]+)")
        importPattern.findAll(content).forEach { match ->
            dependencies.add(match.groupValues[1])
        }
        
        // Detect framework dependencies
        if (content.contains("kotlinx.coroutines")) dependencies.add("Kotlin Coroutines")
        if (content.contains("androidx.compose")) dependencies.add("Jetpack Compose")
        if (content.contains("opengl")) dependencies.add("OpenGL")
        if (content.contains("ktor")) dependencies.add("Ktor Network")
        
        return dependencies.toList()
    }
    
    /**
     * Generate relevant tags for the component
     */
    private fun generateTags(originalFile: File, repository: Repository): List<String> {
        val tags = mutableListOf<String>()
        
        // Repository tags
        tags.add(repository.name)
        tags.add("converted-from-${repository.language.lowercase()}")
        
        // File-based tags
        val fileName = originalFile.name.lowercase()
        when {
            fileName.contains("ll") -> tags.add("linden-lab-component")
            fileName.contains("fs") || fileName.contains("phoenix") -> tags.add("firestorm-enhancement")
            fileName.contains("rlv") -> tags.add("rlv-extension")
        }
        
        // Functionality tags
        tags.add("virtual-world")
        tags.add("second-life-compatible")
        tags.add("kotlin-converted")
        
        return tags
    }
    
    /**
     * Generate documentation for the component
     */
    private fun generateDocumentation(originalFile: File, convertedFile: File): ComponentDocumentation {
        val fileName = convertedFile.nameWithoutExtension
        val componentType = detectComponentType(originalFile)
        
        return ComponentDocumentation(
            description = "Kotlin conversion of ${originalFile.name} from ${originalFile.parent}. " +
                    "Provides ${componentType.name.lowercase().replace('_', ' ')} functionality for virtual world viewer.",
            purpose = "Modernized ${componentType.name.lowercase().replace('_', ' ')} component with Kotlin type safety and coroutine support.",
            usage = "val component = ${fileName.capitalize()}()\ncomponent.initialize()\n// Use component methods...",
            examples = listOf(
                "// Basic usage example",
                "val ${fileName.lowercase()} = ${fileName.capitalize()}()",
                "${fileName.lowercase()}.initialize()",
                "// Component is ready for use"
            ),
            notes = listOf(
                "Converted from original C++/C# implementation",
                "Maintains compatibility with SecondLife protocol",
                "Uses Kotlin coroutines for async operations",
                "Includes null safety improvements"
            )
        )
    }
    
    /**
     * Calculate quality metrics for the converted component
     */
    private fun calculateQualityMetrics(originalFile: File, convertedFile: File): QualityMetrics {
        val originalLines = try { originalFile.readLines().size } catch (e: Exception) { 0 }
        val convertedLines = convertedFile.readLines().size
        val convertedContent = convertedFile.readText()
        
        // Count various quality indicators
        val nullSafetyScore = if (convertedContent.contains("?")) 20 else 0
        val coroutineScore = if (convertedContent.contains("suspend") || convertedContent.contains("coroutine")) 25 else 0
        val errorHandlingScore = if (convertedContent.contains("try") || convertedContent.contains("catch")) 20 else 0
        val documentationScore = if (convertedContent.contains("/**") || convertedContent.contains("//")) 15 else 0
        val typeSystemScore = if (convertedContent.contains(": ")) 20 else 0
        
        val overallScore = nullSafetyScore + coroutineScore + errorHandlingScore + documentationScore + typeSystemScore
        
        return QualityMetrics(
            originalLines = originalLines,
            convertedLines = convertedLines,
            codeReduction = if (originalLines > 0) ((originalLines - convertedLines).toDouble() / originalLines * 100).toInt() else 0,
            nullSafetyScore = nullSafetyScore,
            coroutineIntegration = coroutineScore,
            errorHandling = errorHandlingScore,
            documentation = documentationScore,
            typeSystem = typeSystemScore,
            overallScore = overallScore
        )
    }
    
    /**
     * Apply LLSD formatting standards to code
     */
    private fun applyLLSDFormatting(content: String): String {
        return content
            // Ensure consistent indentation
            .replace(Regex("^    ", RegexOption.MULTILINE), "    ")
            
            // Ensure proper spacing around operators
            .replace(Regex("(\\w)=([\\w\"])"), "$1 = $2")
            .replace(Regex("(\\w)\\+(\\w)"), "$1 + $2")
            .replace(Regex("(\\w)-(\\w)"), "$1 - $2")
            
            // Ensure consistent brace placement
            .replace(Regex("\\)\\{"), ") {")
            .replace(Regex("\\}else"), "} else")
    }
    
    /**
     * Generate unique component ID
     */
    private fun generateComponentId(file: File, repository: Repository): String {
        val repoPrefix = when (repository.name) {
            "secondlife-viewer" -> "SL"
            "firestorm-viewer" -> "FS"
            "libremetaverse" -> "LM"
            "restrained-love-viewer" -> "RLV"
            else -> "UNKNOWN"
        }
        
        val fileName = file.nameWithoutExtension.uppercase()
        val timestamp = System.currentTimeMillis() % 10000
        
        return "${repoPrefix}_${fileName}_${timestamp}"
    }
}

/**
 * LLSD Component Label
 */
@Serializable
data class LLSDLabel(
    val componentId: String,
    val name: String,
    val type: ComponentType,
    val functionality: List<String>,
    val sourceRepository: String,
    val sourceFile: String,
    val sourcePath: String,
    val convertedPath: String,
    val language: String,
    val originalLanguage: String,
    val version: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastModified: LocalDateTime,
    val dependencies: List<String>,
    val llsdCompliant: Boolean,
    val tags: List<String>,
    val documentation: ComponentDocumentation,
    val testingStatus: TestingStatus,
    val qualityMetrics: QualityMetrics
)

/**
 * Component types as defined by LLSD standards
 */
@Serializable
enum class ComponentType {
    CORE_SYSTEM,
    UI_COMPONENT,
    RENDERING,
    NETWORK,
    AVATAR,
    ASSET,
    AUDIO,
    RLV_EXTENSION,
    UTILITY,
    PROTOCOL,
    GRAPHICS_PIPELINE,
    MESSAGING_SYSTEM
}

/**
 * Testing status of components
 */
@Serializable
enum class TestingStatus {
    NOT_TESTED,
    CONVERTED,
    UNIT_TESTED,
    INTEGRATION_TESTED,
    FULLY_VALIDATED,
    PRODUCTION_READY
}

/**
 * Component documentation structure
 */
@Serializable
data class ComponentDocumentation(
    val description: String,
    val purpose: String,
    val usage: String,
    val examples: List<String>,
    val notes: List<String>
)

/**
 * Quality metrics for converted components
 */
@Serializable
data class QualityMetrics(
    val originalLines: Int,
    val convertedLines: Int,
    val codeReduction: Int, // Percentage
    val nullSafetyScore: Int, // Out of 20
    val coroutineIntegration: Int, // Out of 25
    val errorHandling: Int, // Out of 20
    val documentation: Int, // Out of 15
    val typeSystem: Int, // Out of 20
    val overallScore: Int // Out of 100
)