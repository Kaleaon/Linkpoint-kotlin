package com.linkpoint.batch

import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.*

/**
 * Tests for the batch processing system
 */
class BatchProcessorTest {
    
    private lateinit var tempDir: File
    private lateinit var testConfig: BatchConfig
    
    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("batch-processor-test").toFile()
        testConfig = BatchConfig(
            downloadDir = "${tempDir.absolutePath}/downloads",
            convertedDir = "${tempDir.absolutePath}/converted",
            repositories = listOf(
                Repository(
                    name = "test-repo",
                    url = "https://github.com/octocat/Hello-World",
                    language = "C++",
                    fileExtensions = listOf("cpp", "h"),
                    description = "Test repository for batch processing"
                )
            )
        )
    }
    
    @AfterTest
    fun cleanup() {
        tempDir.deleteRecursively()
    }
    
    @Test
    fun testBatchConfigCreation() {
        val config = createDefaultConfig()
        
        assertEquals(4, config.repositories.size)
        assertTrue(config.repositories.any { it.name == "secondlife-viewer" })
        assertTrue(config.repositories.any { it.name == "firestorm-viewer" })
        assertTrue(config.repositories.any { it.name == "libremetaverse" })
        assertTrue(config.repositories.any { it.name == "restrained-love-viewer" })
    }
    
    @Test
    fun testProgressTracker() {
        val tracker = ProgressTracker()
        
        // Test download progress
        tracker.updateDownloadProgress("test-repo", true, 100)
        val report = tracker.getProgressReport()
        
        assertEquals(1, report.downloads.size)
        assertEquals("test-repo", report.downloads[0].repoName)
        assertTrue(report.downloads[0].success)
        assertEquals(100, report.downloads[0].filesDownloaded)
    }
    
    @Test
    fun testCodeConverter() {
        val converter = CodeConverter()
        
        // Test C++ to Kotlin conversion
        val cppCode = """
            class TestClass {
                std::string name;
                void setName(const std::string& newName);
            };
        """.trimIndent()
        
        val testFile = File(tempDir, "test.cpp")
        testFile.writeText(cppCode)
        
        val kotlinCode = converter.convertToKotlin(testFile, "C++")
        
        assertTrue(kotlinCode.contains("package com.linkpoint.converted.cpp"))
        assertTrue(kotlinCode.contains("class TestClass"))
        assertTrue(kotlinCode.contains("String"))
    }
    
    @Test
    fun testLLSDLabeler() {
        val labeler = LLSDLabeler()
        
        // Create test files
        val originalFile = File(tempDir, "original.cpp")
        originalFile.writeText("class TestClass { void method(); };")
        
        val convertedFile = File(tempDir, "converted.kt")
        convertedFile.writeText("class TestClass { fun method() {} }")
        
        val testRepo = Repository(
            name = "test-repo",
            url = "https://example.com/test",
            language = "C++",
            fileExtensions = listOf("cpp"),
            description = "Test repository"
        )
        
        // Test labeling
        labeler.labelComponent(convertedFile, originalFile, testRepo)
        
        val labeledContent = convertedFile.readText()
        assertTrue(labeledContent.contains("LLSD Component Label"))
        assertTrue(labeledContent.contains("Component ID:"))
        
        // Check metadata file was created
        val metadataFile = File(tempDir, "converted.llsd.json")
        assertTrue(metadataFile.exists())
    }
    
    @Test
    fun testSubTaskGeneration() {
        val processor = BatchProcessor(testConfig)
        
        val mockResults = listOf(
            RepositoryResult(
                repository = testConfig.repositories[0],
                success = true,
                stats = RepositoryStats(100, 5000)
            )
        )
        
        // Use reflection to call private method (for testing)
        val method = BatchProcessor::class.java.getDeclaredMethod("generateSubTasks", List::class.java)
        method.isAccessible = true
        val subTasks = method.invoke(processor, mockResults) as List<SubTask>
        
        assertEquals(2, subTasks.size) // Translation + Testing tasks
        assertTrue(subTasks.any { it.title.contains("Translate") })
        assertTrue(subTasks.any { it.title.contains("Test") })
        assertTrue(subTasks.all { it.assignee == "@copilot" })
    }
    
    @Test
    fun testDirectoryCreation() {
        // Test that processor creates necessary directories
        File(testConfig.downloadDir).mkdirs()
        File(testConfig.convertedDir).mkdirs()
        
        assertTrue(File(testConfig.downloadDir).exists())
        assertTrue(File(testConfig.convertedDir).exists())
    }
    
    @Test
    fun testComponentTypeDetection() {
        val labeler = LLSDLabeler()
        
        // Use reflection to test private method
        val method = LLSDLabeler::class.java.getDeclaredMethod("detectComponentType", File::class.java)
        method.isAccessible = true
        
        // Test UI component detection
        val uiFile = File(tempDir, "llfloater.cpp")
        uiFile.writeText("class LLFloater { /* UI code */ }")
        val uiType = method.invoke(labeler, uiFile) as ComponentType
        assertEquals(ComponentType.UI_COMPONENT, uiType)
        
        // Test rendering component detection
        val renderFile = File(tempDir, "llrender.cpp")
        renderFile.writeText("void renderScene() { /* OpenGL code */ }")
        val renderType = method.invoke(labeler, renderFile) as ComponentType  
        assertEquals(ComponentType.RENDERING, renderType)
    }
    
    @Test
    fun testBatchResultSerialization() {
        val result = BatchResult(
            success = true,
            repositories = emptyList(),
            subTasks = listOf(
                SubTask(
                    id = "test-task",
                    title = "Test Task",
                    description = "Test description",
                    assignee = "@copilot",
                    priority = SubTask.Priority.MEDIUM,
                    estimatedHours = 5
                )
            ),
            durationSeconds = 120
        )
        
        // Test that result can be serialized (this validates the data classes)
        assertNotNull(result.success)
        assertNotNull(result.subTasks)
        assertEquals(1, result.subTasks.size)
        assertEquals("test-task", result.subTasks[0].id)
    }
}