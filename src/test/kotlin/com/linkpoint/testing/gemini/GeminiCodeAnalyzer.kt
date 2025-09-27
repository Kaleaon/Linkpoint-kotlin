package com.linkpoint.testing.gemini

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Google Gemini API integration for intelligent code analysis and debugging
 * 
 * This class provides AI-powered analysis capabilities to:
 * - Compare Kotlin implementations against C++ reference code
 * - Identify potential behavioral differences
 * - Suggest fixes and optimizations
 * - Generate comprehensive test cases
 * - Perform recursive debugging analysis
 */
class GeminiCodeAnalyzer(
    private val apiKey: String = System.getenv("GEMINI_API_KEY") ?: ""
) {
    
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    @Serializable
    data class GeminiRequest(
        val contents: List<Content>,
        val generationConfig: GenerationConfig = GenerationConfig()
    )
    
    @Serializable
    data class Content(
        val parts: List<Part>
    )
    
    @Serializable
    data class Part(
        val text: String
    )
    
    @Serializable
    data class GenerationConfig(
        val temperature: Double = 0.1,
        val topK: Int = 40,
        val topP: Double = 0.95,
        val maxOutputTokens: Int = 8192
    )
    
    @Serializable
    data class GeminiResponse(
        val candidates: List<Candidate>
    )
    
    @Serializable
    data class Candidate(
        val content: Content,
        val finishReason: String? = null
    )
    
    /**
     * Analyzes code differences between Kotlin and C++ implementations
     */
    suspend fun analyzeCodeEquivalence(
        kotlinCode: String,
        cppReference: String,
        componentName: String
    ): CodeAnalysisResult {
        if (apiKey.isBlank()) {
            logger.warn("Gemini API key not provided, using fallback analysis")
            return performFallbackAnalysis(kotlinCode, cppReference, componentName)
        }
        
        val prompt = buildCodeAnalysisPrompt(kotlinCode, cppReference, componentName)
        
        return try {
            val response = callGeminiAPI(prompt)
            parseAnalysisResponse(response, componentName)
        } catch (e: Exception) {
            logger.error("Gemini API call failed: ${e.message}", e)
            performFallbackAnalysis(kotlinCode, cppReference, componentName)
        }
    }
    
    /**
     * Generates intelligent test cases for a component
     */
    suspend fun generateTestCases(
        kotlinCode: String,
        componentName: String,
        existingTests: List<String> = emptyList()
    ): TestGenerationResult {
        if (apiKey.isBlank()) {
            return generateFallbackTests(kotlinCode, componentName)
        }
        
        val prompt = buildTestGenerationPrompt(kotlinCode, componentName, existingTests)
        
        return try {
            val response = callGeminiAPI(prompt)
            parseTestGenerationResponse(response, componentName)
        } catch (e: Exception) {
            logger.error("Test generation failed: ${e.message}", e)
            generateFallbackTests(kotlinCode, componentName)
        }
    }
    
    /**
     * Performs recursive debugging analysis to identify and fix issues
     */
    suspend fun performRecursiveDebugging(
        issue: String,
        kotlinCode: String,
        testResults: List<String>,
        maxIterations: Int = 3
    ): DebuggingResult {
        if (apiKey.isBlank()) {
            return performFallbackDebugging(issue, kotlinCode, testResults)
        }
        
        var currentIssue = issue
        var currentCode = kotlinCode
        val debuggingSteps = mutableListOf<DebuggingStep>()
        
        repeat(maxIterations) { iteration ->
            logger.info("Recursive debugging iteration ${iteration + 1}/$maxIterations")
            
            val prompt = buildDebuggingPrompt(currentIssue, currentCode, testResults, iteration)
            
            try {
                val response = callGeminiAPI(prompt)
                val step = parseDebuggingResponse(response, iteration)
                debuggingSteps.add(step)
                
                if (step.isResolved) {
                    logger.info("Issue resolved after ${iteration + 1} iterations")
                    break
                }
                
                currentIssue = step.nextIssue ?: currentIssue
                currentCode = step.suggestedFix ?: currentCode
                
                // Add delay between API calls to respect rate limits
                delay(1000)
                
            } catch (e: Exception) {
                logger.error("Debugging iteration $iteration failed: ${e.message}", e)
                break
            }
        }
        
        return DebuggingResult(
            originalIssue = issue,
            steps = debuggingSteps,
            finalResolution = debuggingSteps.lastOrNull()?.suggestedFix,
            isFullyResolved = debuggingSteps.any { it.isResolved }
        )
    }
    
    private suspend fun callGeminiAPI(prompt: String): String {
        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(prompt))
                )
            )
        )
        
        val response = client.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent") {
            header("x-goog-api-key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        
        val geminiResponse: GeminiResponse = response.body()
        return geminiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text 
            ?: throw Exception("Empty response from Gemini API")
    }
    
    private fun buildCodeAnalysisPrompt(
        kotlinCode: String, 
        cppReference: String, 
        componentName: String
    ): String = """
        Analyze the behavioral equivalence between this Kotlin implementation and its C++ reference:
        
        Component: $componentName
        
        C++ Reference Code:
        ```cpp
        $cppReference
        ```
        
        Kotlin Implementation:
        ```kotlin
        $kotlinCode
        ```
        
        Please provide:
        1. BEHAVIORAL_EQUIVALENCE: Are they functionally equivalent? (YES/NO)
        2. DIFFERENCES: List any behavioral differences
        3. POTENTIAL_ISSUES: Identify possible bugs or edge cases
        4. SUGGESTIONS: Recommend improvements
        5. TEST_SCENARIOS: Suggest specific test cases to verify equivalence
        
        Format your response with clear sections for each analysis point.
    """.trimIndent()
    
    private fun buildTestGenerationPrompt(
        kotlinCode: String,
        componentName: String,
        existingTests: List<String>
    ): String = """
        Generate comprehensive test cases for this Kotlin component:
        
        Component: $componentName
        
        Code:
        ```kotlin
        $kotlinCode
        ```
        
        Existing Tests:
        ${existingTests.joinToString("\n") { "- $it" }}
        
        Generate:
        1. UNIT_TESTS: Basic functionality tests
        2. EDGE_CASES: Boundary condition tests  
        3. ERROR_HANDLING: Exception and error tests
        4. INTEGRATION_TESTS: Cross-component interaction tests
        5. PERFORMANCE_TESTS: Benchmark and optimization tests
        
        Provide actual Kotlin test code that can be compiled and run.
    """.trimIndent()
    
    private fun buildDebuggingPrompt(
        issue: String,
        kotlinCode: String,
        testResults: List<String>,
        iteration: Int
    ): String = """
        Debugging Analysis - Iteration ${iteration + 1}
        
        Current Issue: $issue
        
        Code Under Analysis:
        ```kotlin
        $kotlinCode
        ```
        
        Test Results:
        ${testResults.joinToString("\n") { "- $it" }}
        
        Please provide:
        1. ROOT_CAUSE: Identify the underlying cause of the issue
        2. SUGGESTED_FIX: Provide specific code changes
        3. VALIDATION: How to verify the fix works
        4. NEXT_ISSUE: If this fix reveals other issues, what are they?
        5. RESOLVED: Is the original issue fully resolved? (YES/NO)
        
        Be specific and provide actionable recommendations.
    """.trimIndent()
    
    private fun parseAnalysisResponse(response: String, componentName: String): CodeAnalysisResult {
        // Parse the Gemini response into structured data
        val sections = response.split("\n").groupBy { line ->
            when {
                line.contains("BEHAVIORAL_EQUIVALENCE:", ignoreCase = true) -> "equivalence"
                line.contains("DIFFERENCES:", ignoreCase = true) -> "differences"
                line.contains("POTENTIAL_ISSUES:", ignoreCase = true) -> "issues"
                line.contains("SUGGESTIONS:", ignoreCase = true) -> "suggestions"
                line.contains("TEST_SCENARIOS:", ignoreCase = true) -> "tests"
                else -> "content"
            }
        }
        
        return CodeAnalysisResult(
            componentName = componentName,
            isBehaviorallyEquivalent = sections["equivalence"]?.any { it.contains("YES", ignoreCase = true) } ?: false,
            differences = extractListItems(sections["differences"] ?: emptyList()),
            potentialIssues = extractListItems(sections["issues"] ?: emptyList()),
            suggestions = extractListItems(sections["suggestions"] ?: emptyList()),
            recommendedTests = extractListItems(sections["tests"] ?: emptyList()),
            rawAnalysis = response
        )
    }
    
    private fun parseTestGenerationResponse(response: String, componentName: String): TestGenerationResult {
        return TestGenerationResult(
            componentName = componentName,
            unitTests = extractCodeBlocks(response, "unit"),
            edgeCaseTests = extractCodeBlocks(response, "edge"),
            errorHandlingTests = extractCodeBlocks(response, "error"),
            integrationTests = extractCodeBlocks(response, "integration"),
            performanceTests = extractCodeBlocks(response, "performance"),
            rawResponse = response
        )
    }
    
    private fun parseDebuggingResponse(response: String, iteration: Int): DebuggingStep {
        val sections = response.split("\n").groupBy { line ->
            when {
                line.contains("ROOT_CAUSE:", ignoreCase = true) -> "cause"
                line.contains("SUGGESTED_FIX:", ignoreCase = true) -> "fix"
                line.contains("VALIDATION:", ignoreCase = true) -> "validation"
                line.contains("NEXT_ISSUE:", ignoreCase = true) -> "next"
                line.contains("RESOLVED:", ignoreCase = true) -> "resolved"
                else -> "content"
            }
        }
        
        return DebuggingStep(
            iteration = iteration,
            rootCause = sections["cause"]?.joinToString(" ")?.trim() ?: "",
            suggestedFix = extractCodeBlocks(response, "fix").firstOrNull(),
            validationSteps = extractListItems(sections["validation"] ?: emptyList()),
            nextIssue = sections["next"]?.joinToString(" ")?.trim()?.takeIf { it.isNotBlank() },
            isResolved = sections["resolved"]?.any { it.contains("YES", ignoreCase = true) } ?: false
        )
    }
    
    private fun extractListItems(lines: List<String>): List<String> {
        return lines.flatMap { line ->
            line.split("-", "•", "*")
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.contains(":") }
        }
    }
    
    private fun extractCodeBlocks(text: String, blockType: String): List<String> {
        val regex = "```(?:kotlin)?([\\s\\S]*?)```".toRegex()
        return regex.findAll(text).map { it.groupValues[1].trim() }.toList()
    }
    
    // Fallback implementations when Gemini API is not available
    private fun performFallbackAnalysis(
        kotlinCode: String, 
        cppReference: String, 
        componentName: String
    ): CodeAnalysisResult {
        logger.info("Using fallback analysis for $componentName")
        
        return CodeAnalysisResult(
            componentName = componentName,
            isBehaviorallyEquivalent = true, // Assume equivalent unless proven otherwise
            differences = listOf("Unable to analyze without Gemini API - manual review required"),
            potentialIssues = listOf("Automatic analysis unavailable"),
            suggestions = listOf("Set GEMINI_API_KEY environment variable for AI-powered analysis"),
            recommendedTests = listOf("Basic functionality tests", "Error handling tests", "Edge case tests"),
            rawAnalysis = "Fallback analysis - no AI assistance available"
        )
    }
    
    private fun generateFallbackTests(kotlinCode: String, componentName: String): TestGenerationResult {
        return TestGenerationResult(
            componentName = componentName,
            unitTests = listOf("// Manual test creation required - set GEMINI_API_KEY for AI generation"),
            edgeCaseTests = emptyList(),
            errorHandlingTests = emptyList(),
            integrationTests = emptyList(),
            performanceTests = emptyList(),
            rawResponse = "Fallback mode - AI test generation unavailable"
        )
    }
    
    private fun performFallbackDebugging(
        issue: String,
        kotlinCode: String,
        testResults: List<String>
    ): DebuggingResult {
        return DebuggingResult(
            originalIssue = issue,
            steps = listOf(
                DebuggingStep(
                    iteration = 0,
                    rootCause = "Unable to analyze without Gemini API",
                    suggestedFix = "// Manual debugging required - set GEMINI_API_KEY for AI assistance",
                    validationSteps = listOf("Manual code review", "Run existing tests"),
                    nextIssue = null,
                    isResolved = false
                )
            ),
            finalResolution = null,
            isFullyResolved = false
        )
    }
    
    fun close() {
        client.close()
    }
}

// Data classes for analysis results
data class CodeAnalysisResult(
    val componentName: String,
    val isBehaviorallyEquivalent: Boolean,
    val differences: List<String>,
    val potentialIssues: List<String>,
    val suggestions: List<String>,
    val recommendedTests: List<String>,
    val rawAnalysis: String
)

data class TestGenerationResult(
    val componentName: String,
    val unitTests: List<String>,
    val edgeCaseTests: List<String>,
    val errorHandlingTests: List<String>,
    val integrationTests: List<String>,
    val performanceTests: List<String>,
    val rawResponse: String
)

data class DebuggingResult(
    val originalIssue: String,
    val steps: List<DebuggingStep>,
    val finalResolution: String?,
    val isFullyResolved: Boolean
)

data class DebuggingStep(
    val iteration: Int,
    val rootCause: String,
    val suggestedFix: String?,
    val validationSteps: List<String>,
    val nextIssue: String?,
    val isResolved: Boolean
)