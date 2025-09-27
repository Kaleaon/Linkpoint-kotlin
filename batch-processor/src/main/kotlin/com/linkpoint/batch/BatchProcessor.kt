package com.linkpoint.batch

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.eclipse.jgit.api.Git
import java.io.File
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Main batch processor for downloading and converting virtual world viewer codebases
 * to Kotlin-compatible components with LLSD standards labeling.
 */
class BatchProcessor(private val config: BatchConfig) {
    
    private val json = Json { prettyPrint = true }
    private val progressTracker = ProgressTracker()
    private val converter = CodeConverter()
    private val llsdLabeler = LLSDLabeler()
    
    /**
     * Execute the complete batch processing workflow
     */
    suspend fun execute(): BatchResult = withContext(Dispatchers.IO) {
        println("🚀 Starting Batch Download and Conversion Process")
        println("═══════════════════════════════════════════════")
        
        val startTime = System.currentTimeMillis()
        val results = mutableListOf<RepositoryResult>()
        
        try {
            // Phase 1: Download all repositories
            println("\n📥 Phase 1: Downloading Source Repositories")
            for (repo in config.repositories) {
                val result = downloadRepository(repo)
                results.add(result)
                progressTracker.updateDownloadProgress(repo.name, result.success)
            }
            
            // Phase 2: Convert to Kotlin
            println("\n🔄 Phase 2: Converting to Kotlin-Compatible Components")
            for (result in results.filter { it.success }) {
                convertRepository(result)
                progressTracker.updateConversionProgress(result.repository.name, true)
            }
            
            // Phase 3: Apply LLSD Standards
            println("\n🏷️ Phase 3: Applying LLSD Standards and Labeling")
            applyLLSDStandards()
            
            // Phase 4: Generate Sub-tasks
            println("\n📋 Phase 4: Generating Sub-tasks for @copilot")
            val subTasks = generateSubTasks(results)
            
            val endTime = System.currentTimeMillis()
            val duration = (endTime - startTime) / 1000
            
            println("\n✅ Batch Processing Complete!")
            println("Total time: ${duration}s")
            progressTracker.printSummary()
            
            return@withContext BatchResult(true, results, subTasks, duration)
            
        } catch (e: Exception) {
            println("\n❌ Batch processing failed: ${e.message}")
            return@withContext BatchResult(false, results, emptyList(), 0, e.message)
        }
    }
    
    /**
     * Download a single repository
     */
    private suspend fun downloadRepository(repo: Repository): RepositoryResult = withContext(Dispatchers.IO) {
        println("  📦 Downloading ${repo.name} from ${repo.url}")
        
        val downloadDir = File(config.downloadDir, repo.name)
        downloadDir.mkdirs()
        
        try {
            // Clone the repository
            val git = Git.cloneRepository()
                .setURI(repo.url)
                .setDirectory(downloadDir)
                .setBranch(repo.branch)
                .call()
            
            val stats = analyzeRepository(downloadDir)
            println("    ✅ Downloaded ${stats.totalFiles} files (${stats.totalLines} lines)")
            
            git.close()
            
            return@withContext RepositoryResult(
                repository = repo,
                success = true,
                downloadPath = downloadDir.toPath(),
                stats = stats
            )
            
        } catch (e: Exception) {
            println("    ❌ Failed to download ${repo.name}: ${e.message}")
            return@withContext RepositoryResult(
                repository = repo,
                success = false,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Convert a repository to Kotlin-compatible components
     */
    private suspend fun convertRepository(result: RepositoryResult) = withContext(Dispatchers.IO) {
        if (!result.success || result.downloadPath == null) return@withContext
        
        println("  🔄 Converting ${result.repository.name} to Kotlin")
        
        val outputDir = File(config.convertedDir, result.repository.name)
        outputDir.mkdirs()
        
        // Find all source files to convert
        val sourceFiles = findSourceFiles(result.downloadPath!!.toFile(), result.repository.fileExtensions)
        
        var convertedCount = 0
        var debuggedCount = 0
        
        for (sourceFile in sourceFiles) {
            try {
                // Convert the file
                val kotlinCode = converter.convertToKotlin(sourceFile, result.repository.language)
                
                // Debug and validate
                val debuggedCode = converter.debugAndValidate(kotlinCode, sourceFile)
                
                // Save converted file
                val relativePath = result.downloadPath!!.relativize(sourceFile.toPath())
                val outputFile = File(outputDir, relativePath.toString().replace(
                    Regex("\\.(cpp|c|h|cs)$"), ".kt"
                ))
                
                outputFile.parentFile.mkdirs()
                outputFile.writeText(debuggedCode)
                
                convertedCount++
                if (debuggedCode != kotlinCode) {
                    debuggedCount++
                }
                
                // Apply LLSD labeling
                llsdLabeler.labelComponent(outputFile, sourceFile, result.repository)
                
            } catch (e: Exception) {
                println("    ⚠️ Failed to convert ${sourceFile.name}: ${e.message}")
            }
        }
        
        println("    ✅ Converted $convertedCount files, debugged $debuggedCount components")
    }
    
    /**
     * Apply LLSD standards to all converted components
     */
    private suspend fun applyLLSDStandards() = withContext(Dispatchers.IO) {
        println("  🏷️ Applying LLSD standards to converted components")
        
        val convertedDir = File(config.convertedDir)
        if (!convertedDir.exists()) return@withContext
        
        val allKotlinFiles = convertedDir.walkTopDown()
            .filter { it.extension == "kt" }
            .toList()
        
        var labeledCount = 0
        
        for (file in allKotlinFiles) {
            try {
                llsdLabeler.applyLLSDStandards(file)
                labeledCount++
            } catch (e: Exception) {
                println("    ⚠️ Failed to apply LLSD standards to ${file.name}: ${e.message}")
            }
        }
        
        println("    ✅ Applied LLSD standards to $labeledCount components")
    }
    
    /**
     * Generate sub-tasks for copilot
     */
    private fun generateSubTasks(results: List<RepositoryResult>): List<SubTask> {
        val tasks = mutableListOf<SubTask>()
        
        for (result in results.filter { it.success }) {
            // Translation task
            tasks.add(SubTask(
                id = "translate-${result.repository.name}",
                title = "Translate ${result.repository.name} Components",
                description = "Review and refine Kotlin translation of ${result.stats?.totalFiles ?: 0} files from ${result.repository.name}",
                assignee = "@copilot",
                priority = when (result.repository.name) {
                    "secondlife-viewer" -> SubTask.Priority.HIGH
                    "firestorm-viewer" -> SubTask.Priority.HIGH
                    else -> SubTask.Priority.MEDIUM
                },
                estimatedHours = (result.stats?.totalFiles ?: 0) / 10
            ))
            
            // Testing task
            tasks.add(SubTask(
                id = "test-${result.repository.name}",
                title = "Test ${result.repository.name} Components",
                description = "Create and execute tests for converted ${result.repository.name} components",
                assignee = "@copilot",
                priority = SubTask.Priority.MEDIUM,
                estimatedHours = (result.stats?.totalFiles ?: 0) / 20
            ))
        }
        
        return tasks
    }
    
    private fun analyzeRepository(dir: File): RepositoryStats {
        var totalFiles = 0
        var totalLines = 0
        
        dir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension in listOf("cpp", "c", "h", "cs", "kt")) {
                totalFiles++
                try {
                    totalLines += file.readLines().size
                } catch (e: Exception) {
                    // Ignore files that can't be read
                }
            }
        }
        
        return RepositoryStats(totalFiles, totalLines)
    }
    
    private fun findSourceFiles(dir: File, extensions: List<String>): List<File> {
        return dir.walkTopDown()
            .filter { it.isFile && it.extension in extensions }
            .toList()
    }
}

/**
 * Configuration for batch processing
 */
@Serializable
data class BatchConfig(
    val downloadDir: String = "batch-processor/downloads",
    val convertedDir: String = "batch-processor/converted",
    val repositories: List<Repository>
)

/**
 * Repository configuration
 */
@Serializable
data class Repository(
    val name: String,
    val url: String,
    val branch: String = "main",
    val language: String,
    val fileExtensions: List<String>,
    val description: String
)

/**
 * Result of processing a single repository
 */
data class RepositoryResult(
    val repository: Repository,
    val success: Boolean,
    val downloadPath: Path? = null,
    val stats: RepositoryStats? = null,
    val errorMessage: String? = null
)

/**
 * Statistics about a repository
 */
data class RepositoryStats(
    val totalFiles: Int,
    val totalLines: Int
)

/**
 * Overall batch processing result
 */
data class BatchResult(
    val success: Boolean,
    val repositories: List<RepositoryResult>,
    val subTasks: List<SubTask>,
    val durationSeconds: Long,
    val errorMessage: String? = null
)

/**
 * Sub-task for copilot assignment
 */
@Serializable
data class SubTask(
    val id: String,
    val title: String,
    val description: String,
    val assignee: String,
    val priority: Priority,
    val estimatedHours: Int,
    val createdAt: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
) {
    enum class Priority { LOW, MEDIUM, HIGH, CRITICAL }
}

/**
 * Main entry point
 */
suspend fun main() {
    val config = createDefaultConfig()
    val processor = BatchProcessor(config)
    
    val result = processor.execute()
    
    if (result.success) {
        println("\n🎉 Batch processing completed successfully!")
        println("Generated ${result.subTasks.size} sub-tasks for @copilot")
        
        // Save results to JSON
        val json = Json { prettyPrint = true }
        File("batch-processor/results.json").writeText(
            json.encodeToString(BatchResult.serializer(), result)
        )
        
    } else {
        println("\n💥 Batch processing failed: ${result.errorMessage}")
        System.exit(1)
    }
}

/**
 * Create default configuration for all target repositories
 */
fun createDefaultConfig(): BatchConfig {
    return BatchConfig(
        repositories = listOf(
            Repository(
                name = "secondlife-viewer",
                url = "https://github.com/secondlife/viewer",
                language = "C++",
                fileExtensions = listOf("cpp", "c", "h"),
                description = "Official Second Life viewer with core virtual world functionality"
            ),
            Repository(
                name = "firestorm-viewer",
                url = "https://github.com/FirestormViewer/phoenix-firestorm",
                language = "C++",
                fileExtensions = listOf("cpp", "c", "h"),
                description = "Popular third-party viewer with advanced features and optimizations"
            ),
            Repository(
                name = "libremetaverse",
                url = "https://github.com/openmetaversefoundation/libopenmetaverse",
                language = "C#",
                fileExtensions = listOf("cs"),
                description = "C# library for virtual world protocols and functionality"
            ),
            Repository(
                name = "restrained-love-viewer",
                url = "https://github.com/RestrainedLove/RestrainedLove",
                language = "C++",
                fileExtensions = listOf("cpp", "c", "h"),
                description = "Specialized viewer with RLV protocol extensions"
            )
        )
    )
}