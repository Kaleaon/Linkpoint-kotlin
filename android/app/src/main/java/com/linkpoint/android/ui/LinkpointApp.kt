package com.linkpoint.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linkpoint.android.viewmodel.LinkpointViewModel

/**
 * Main Compose UI for the Linkpoint Android Virtual World Viewer
 * 
 * This composable provides the complete mobile interface inspired by Lumiya Viewer
 * with modern Material Design 3 components and touch-optimized interaction patterns.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkpointApp(
    viewModel: LinkpointViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Linkpoint Virtual World Viewer",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Modern Kotlin Implementation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Based on SecondLife, Firestorm & RLV Viewers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Status Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Implementation Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                StatusItem("Protocol System", uiState.protocolStatus, Icons.Default.Link)
                StatusItem("Graphics Pipeline", uiState.graphicsStatus, Icons.Default.Visibility)
                StatusItem("Mobile UI", uiState.uiStatus, Icons.Default.PhoneAndroid)
                StatusItem("Asset Management", uiState.assetStatus, Icons.Default.Storage)
                StatusItem("Audio System", uiState.audioStatus, Icons.Default.VolumeUp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Feature Demonstration Buttons
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Interactive Demonstrations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DemoButton(
                        text = "Mobile UI",
                        icon = Icons.Default.PhoneAndroid,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.demonstrateMobileUI()
                    }
                    
                    DemoButton(
                        text = "Protocol",
                        icon = Icons.Default.Link,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.demonstrateProtocol()
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DemoButton(
                        text = "Graphics",
                        icon = Icons.Default.Visibility,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.demonstrateGraphics()
                    }
                    
                    DemoButton(
                        text = "Audio",
                        icon = Icons.Default.VolumeUp,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.demonstrateAudio()
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Activity Log
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Activity Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.activityLog) { logEntry ->
                        Text(
                            text = logEntry,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusItem(
    title: String,
    status: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = when (status) {
                "Complete" -> MaterialTheme.colorScheme.primary
                "Active" -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = status,
            style = MaterialTheme.typography.bodySmall,
            color = when (status) {
                "Complete" -> MaterialTheme.colorScheme.primary
                "Active" -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DemoButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium
        )
    }
}