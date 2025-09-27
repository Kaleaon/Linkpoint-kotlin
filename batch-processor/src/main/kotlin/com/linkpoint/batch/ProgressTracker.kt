package com.linkpoint.batch

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Tracks progress of batch download and conversion operations
 * Provides transparency and monitoring for the batch processing workflow
 */
class ProgressTracker {
    
    private val downloads = mutableMapOf<String, DownloadProgress>()
    private val conversions = mutableMapOf<String, ConversionProgress>()
    private val startTime = System.currentTimeMillis()
    
    /**
     * Update download progress for a repository
     */
    fun updateDownloadProgress(repoName: String, success: Boolean, files: Int = 0, error: String? = null) {
        downloads[repoName] = DownloadProgress(
            repoName = repoName,
            success = success,
            filesDownloaded = files,
            timestamp = LocalDateTime.now(),
            error = error
        )
        
        val status = if (success) "✅" else "❌"
        println("    $status $repoName: ${if (success) "$files files downloaded" else "failed - $error"}")
    }
    
    /**
     * Update conversion progress for a repository
     */
    fun updateConversionProgress(repoName: String, success: Boolean, converted: Int = 0, debugged: Int = 0, error: String? = null) {
        conversions[repoName] = ConversionProgress(
            repoName = repoName,
            success = success,
            filesConverted = converted,
            filesDebugged = debugged,
            timestamp = LocalDateTime.now(),
            error = error
        )
        
        val status = if (success) "✅" else "❌"
        println("    $status $repoName: ${if (success) "$converted converted, $debugged debugged" else "failed - $error"}")
    }
    
    /**
     * Print comprehensive progress summary
     */
    fun printSummary() {
        val totalTime = (System.currentTimeMillis() - startTime) / 1000
        
        println("\n📊 BATCH PROCESSING SUMMARY")
        println("═══════════════════════════════════════")
        println("Total Processing Time: ${totalTime}s")
        
        // Download Summary
        println("\n📥 Download Results:")
        val successfulDownloads = downloads.values.count { it.success }
        val totalDownloads = downloads.size
        val totalFilesDownloaded = downloads.values.sumOf { it.filesDownloaded }
        
        println("  Success Rate: $successfulDownloads/$totalDownloads repositories")
        println("  Total Files Downloaded: $totalFilesDownloaded")
        
        downloads.values.forEach { progress ->
            val status = if (progress.success) "✅" else "❌"
            val details = if (progress.success) {
                "${progress.filesDownloaded} files"
            } else {
                "Error: ${progress.error}"
            }
            println("    $status ${progress.repoName}: $details")
        }
        
        // Conversion Summary
        println("\n🔄 Conversion Results:")
        val successfulConversions = conversions.values.count { it.success }
        val totalConversions = conversions.size
        val totalFilesConverted = conversions.values.sumOf { it.filesConverted }
        val totalFilesDebugged = conversions.values.sumOf { it.filesDebugged }
        
        println("  Success Rate: $successfulConversions/$totalConversions repositories")
        println("  Total Files Converted: $totalFilesConverted")
        println("  Total Files Debugged: $totalFilesDebugged")
        
        conversions.values.forEach { progress ->
            val status = if (progress.success) "✅" else "❌"
            val details = if (progress.success) {
                "${progress.filesConverted} converted, ${progress.filesDebugged} debugged"
            } else {
                "Error: ${progress.error}"
            }
            println("    $status ${progress.repoName}: $details")
        }
        
        // Performance Metrics
        println("\n⚡ Performance Metrics:")
        val avgFilesPerSecond = if (totalTime > 0) totalFilesDownloaded / totalTime else 0
        println("  Average Download Speed: $avgFilesPerSecond files/second")
        
        val avgConversionPerSecond = if (totalTime > 0) totalFilesConverted / totalTime else 0
        println("  Average Conversion Speed: $avgConversionPerSecond files/second")
        
        // Quality Metrics
        println("\n🎯 Quality Metrics:")
        val debuggingRate = if (totalFilesConverted > 0) {
            (totalFilesDebugged.toDouble() / totalFilesConverted * 100).toInt()
        } else 0
        println("  Code Debugging Rate: $debuggingRate%")
        
        val overallSuccessRate = if (totalDownloads > 0) {
            (successfulDownloads.toDouble() / totalDownloads * 100).toInt()
        } else 0
        println("  Overall Success Rate: $overallSuccessRate%")
    }
    
    /**
     * Get current progress as a structured report
     */
    fun getProgressReport(): ProgressReport {
        return ProgressReport(
            downloads = downloads.values.toList(),
            conversions = conversions.values.toList(),
            startTime = startTime,
            currentTime = System.currentTimeMillis()
        )
    }
    
    /**
     * Export progress to JSON for external monitoring
     */
    fun exportProgress(): String {
        val report = getProgressReport()
        return kotlinx.serialization.json.Json { prettyPrint = true }
            .encodeToString(ProgressReport.serializer(), report)
    }
}

/**
 * Progress information for a single download operation
 */
@kotlinx.serialization.Serializable
data class DownloadProgress(
    val repoName: String,
    val success: Boolean,
    val filesDownloaded: Int,
    @kotlinx.serialization.Serializable(with = LocalDateTimeSerializer::class)
    val timestamp: LocalDateTime,
    val error: String? = null
)

/**
 * Progress information for a single conversion operation
 */
@kotlinx.serialization.Serializable
data class ConversionProgress(
    val repoName: String,
    val success: Boolean,
    val filesConverted: Int,
    val filesDebugged: Int,
    @kotlinx.serialization.Serializable(with = LocalDateTimeSerializer::class)
    val timestamp: LocalDateTime,
    val error: String? = null
)

/**
 * Complete progress report
 */
@kotlinx.serialization.Serializable
data class ProgressReport(
    val downloads: List<DownloadProgress>,
    val conversions: List<ConversionProgress>,
    val startTime: Long,
    val currentTime: Long
) {
    val durationSeconds: Long get() = (currentTime - startTime) / 1000
    val totalRepositories: Int get() = downloads.size
    val successfulDownloads: Int get() = downloads.count { it.success }
    val successfulConversions: Int get() = conversions.count { it.success }
    val totalFilesProcessed: Int get() = downloads.sumOf { it.filesDownloaded }
    val totalFilesConverted: Int get() = conversions.sumOf { it.filesConverted }
}

/**
 * Custom serializer for LocalDateTime
 */
object LocalDateTimeSerializer : kotlinx.serialization.KSerializer<LocalDateTime> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("LocalDateTime", kotlinx.serialization.descriptors.PrimitiveKind.STRING)
    
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    }
    
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}