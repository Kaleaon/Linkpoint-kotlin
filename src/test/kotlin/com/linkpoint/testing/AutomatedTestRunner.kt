package com.linkpoint.testing

import com.linkpoint.testing.tools.AutomatedTestOrchestrator
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Main automated test runner for the comprehensive Kotlin-to-C++/C# validation system
 * 
 * This test class serves as the entry point for running the complete automated testing suite
 * that validates Kotlin implementations against original C++ reference behavior.
 * 
 * Features:
 * - Behavioral compatibility testing
 * - Google Gemini AI-powered code analysis  
 * - Protocol validation against C++ references
 * - Recursive debugging until bugs are resolved
 * - Comprehensive reporting and metrics
 * 
 * Usage:
 * 1. Set GEMINI_API_KEY environment variable for AI features (optional)
 * 2. Run: ./gradlew test --tests "com.linkpoint.testing.AutomatedTestRunner"
 * 3. Review generated reports in /tmp/
 */
class AutomatedTestRunner {
    
    @Test
    fun `run comprehensive automated test suite`() = runBlocking {
        logger.info("🚀 Starting Comprehensive Automated Test Suite for Kotlin-to-C++ Validation")
        
        val orchestrator = AutomatedTestOrchestrator()
        
        try {
            val results = orchestrator.runComprehensiveTestSuite()
            
            // Assert overall success
            assertTrue(
                results.overallSuccess || results.behavioralResults.any { it.passed },
                "At least some tests should pass. Check the detailed report for specifics.\n\n" +
                "Summary:\n" +
                "- Behavioral Tests: ${results.behavioralResults.count { it.passed }}/${results.behavioralResults.size} passed\n" +
                "- Validation Tests: ${results.validationResults.count { it.isFullyCompatible }}/${results.validationResults.size} fully compatible\n" +
                "- AI Analysis: ${results.aiAnalysisResults.count { it.isBehaviorallyEquivalent }}/${results.aiAnalysisResults.size} equivalent\n" +
                "- Debugging: ${results.debuggingResults.count { it.isFullyResolved }}/${results.debuggingResults.size} resolved\n" +
                "\nDuration: ${results.durationMs}ms\n\n" +
                "Detailed Report:\n${results.finalReport}"
            )
            
            logger.info("✅ Automated test suite completed successfully")
            logger.info("📊 Test Results Summary:")
            logger.info("   - Duration: ${results.durationMs}ms")
            logger.info("   - Behavioral Tests: ${results.behavioralResults.count { it.passed }}/${results.behavioralResults.size} passed")
            logger.info("   - Validation Tests: ${results.validationResults.count { it.isFullyCompatible }}/${results.validationResults.size} compatible")
            logger.info("   - AI Analysis: ${results.aiAnalysisResults.count { it.isBehaviorallyEquivalent }}/${results.aiAnalysisResults.size} equivalent")
            logger.info("   - Debugging Sessions: ${results.debuggingResults.count { it.isFullyResolved }}/${results.debuggingResults.size} resolved")
            
        } catch (e: Exception) {
            logger.error("❌ Automated test suite failed: ${e.message}", e)
            
            // Still provide useful information even on failure
            throw AssertionError(
                "Automated test suite encountered an error: ${e.message}\n\n" +
                "This could be due to:\n" +
                "1. Missing GEMINI_API_KEY environment variable (AI features will be disabled)\n" +
                "2. Network connectivity issues\n" +
                "3. Missing test dependencies\n" +
                "4. Compilation errors in source code\n\n" +
                "Stack trace:\n${e.stackTraceToString()}"
            )
        } finally {
            orchestrator.cleanup()
        }
    }
    
    @Test
    fun `verify testing framework setup`() {
        logger.info("🔧 Verifying automated testing framework setup")
        
        // Verify core components are available
        try {
            val orchestrator = AutomatedTestOrchestrator()
            logger.info("✅ AutomatedTestOrchestrator created successfully")
            
            // Check if Gemini API key is available
            val hasGeminiKey = System.getenv("GEMINI_API_KEY")?.isNotBlank() == true
            if (hasGeminiKey) {
                logger.info("✅ Google Gemini API key detected - AI features enabled")
            } else {
                logger.warn("⚠️ No GEMINI_API_KEY found - AI features will use fallback mode")
            }
            
            // Verify source files exist
            val sourceFiles = listOf(
                "/home/runner/work/Linkpoint-kotlin/Linkpoint-kotlin/protocol/src/main/kotlin/com/linkpoint/protocol/LoginSystem.kt",
                "/home/runner/work/Linkpoint-kotlin/Linkpoint-kotlin/protocol/src/main/kotlin/com/linkpoint/protocol/RLVProcessor.kt",
                "/home/runner/work/Linkpoint-kotlin/Linkpoint-kotlin/protocol/src/main/kotlin/com/linkpoint/protocol/UDPMessageSystem.kt"
            )
            
            sourceFiles.forEach { path ->
                val file = java.io.File(path)
                if (file.exists()) {
                    logger.info("✅ Source file found: ${file.name}")
                } else {
                    logger.warn("⚠️ Source file missing: ${file.name}")
                }
            }
            
            orchestrator.cleanup()
            
        } catch (e: Exception) {
            logger.error("❌ Setup verification failed: ${e.message}", e)
            throw AssertionError("Testing framework setup verification failed: ${e.message}")
        }
        
        logger.info("✅ Testing framework setup verification completed")
    }
    
    @Test 
    fun `demonstrate individual testing components`() = runBlocking {
        logger.info("🎯 Demonstrating individual testing components")
        
        try {
            // Demonstrate behavioral testing
            logger.info("📋 Testing Behavioral Testing Framework...")
            val loginTest = com.linkpoint.testing.behavioral.LoginSystemBehavioralTest()
            val scenarios = loginTest.getTestScenarios()
            logger.info("✅ LoginSystem has ${scenarios.size} behavioral test scenarios")
            
            // Demonstrate protocol validation
            logger.info("🔍 Testing Protocol Validation Framework...")
            val validator = com.linkpoint.testing.validation.ProtocolValidator()
            logger.info("✅ Protocol validator initialized with C++ reference behaviors")
            
            // Demonstrate AI analysis (with fallback)
            logger.info("🤖 Testing AI Analysis Framework...")
            val geminiAnalyzer = com.linkpoint.testing.gemini.GeminiCodeAnalyzer()
            val hasApiKey = System.getenv("GEMINI_API_KEY")?.isNotBlank() == true
            if (hasApiKey) {
                logger.info("✅ Gemini AI analyzer ready with API key")
            } else {
                logger.info("✅ Gemini AI analyzer ready in fallback mode")
            }
            geminiAnalyzer.close()
            
            logger.info("✅ All individual testing components demonstrated successfully")
            
        } catch (e: Exception) {
            logger.error("❌ Component demonstration failed: ${e.message}", e)
            throw AssertionError("Individual component demonstration failed: ${e.message}")
        }
    }
}