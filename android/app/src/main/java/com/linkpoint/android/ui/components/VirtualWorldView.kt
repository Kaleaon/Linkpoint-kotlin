package com.linkpoint.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Virtual World View Component
 * 
 * Provides a 3D world view simulation for the Android app,
 * demonstrating camera controls and world interaction patterns
 * from SecondLife, Firestorm, and RLV viewers.
 */
@Composable
fun VirtualWorldView(
    modifier: Modifier = Modifier,
    onCameraMove: (x: Float, y: Float, z: Float) -> Unit = { _, _, _ -> },
    onObjectTap: (objectId: String) -> Unit = { }
) {
    var cameraPosition by remember { mutableStateOf(Offset(0f, 0f)) }
    var cameraZoom by remember { mutableStateOf(1f) }
    var worldObjects by remember { 
        mutableStateOf(listOf(
            WorldObject("avatar_1", "Avatar", Offset(100f, 100f), Color.Blue),
            WorldObject("object_1", "Cube", Offset(200f, 150f), Color.Red),
            WorldObject("object_2", "Sphere", Offset(300f, 200f), Color.Green),
            WorldObject("terrain_1", "Ground", Offset(150f, 250f), Color.Brown)
        ))
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF87CEEB)) // Sky blue background
            .clipToBounds()
    ) {
        // 3D World Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        cameraPosition = Offset(
                            cameraPosition.x + dragAmount.x,
                            cameraPosition.y + dragAmount.y
                        )
                        onCameraMove(cameraPosition.x, cameraPosition.y, cameraZoom)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Find tapped object
                        worldObjects.forEach { obj ->
                            val objScreen = Offset(
                                obj.position.x + cameraPosition.x,
                                obj.position.y + cameraPosition.y
                            )
                            val distance = sqrt(
                                (offset.x - objScreen.x).pow(2) + 
                                (offset.y - objScreen.y).pow(2)
                            )
                            if (distance < 30f) {
                                onObjectTap(obj.id)
                            }
                        }
                    }
                }
        ) {
            drawVirtualWorld(worldObjects, cameraPosition, cameraZoom)
        }
        
        // Camera Controls Overlay
        CameraControlsOverlay(
            cameraPosition = cameraPosition,
            cameraZoom = cameraZoom,
            onZoomChange = { zoom ->
                cameraZoom = zoom.coerceIn(0.5f, 3f)
                onCameraMove(cameraPosition.x, cameraPosition.y, cameraZoom)
            },
            modifier = Modifier.align(Alignment.BottomEnd)
        )
        
        // World Info Overlay
        WorldInfoOverlay(
            cameraPosition = cameraPosition,
            objectCount = worldObjects.size,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}

@Composable
private fun CameraControlsOverlay(
    cameraPosition: Offset,
    cameraZoom: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Camera Controls",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Zoom Control
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Zoom:",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Slider(
                    value = cameraZoom,
                    onValueChange = onZoomChange,
                    valueRange = 0.5f..3f,
                    modifier = Modifier.width(100.dp)
                )
            }
            
            Text(
                text = "Drag to move camera",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WorldInfoOverlay(
    cameraPosition: Offset,
    objectCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Virtual World",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Objects: $objectCount",
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                text = "Camera: (${cameraPosition.x.toInt()}, ${cameraPosition.y.toInt()})",
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                text = "Tap objects to interact",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun DrawScope.drawVirtualWorld(
    worldObjects: List<WorldObject>,
    cameraPosition: Offset,
    cameraZoom: Float
) {
    // Draw grid pattern for ground
    val gridSize = 50f * cameraZoom
    val offsetX = cameraPosition.x % gridSize
    val offsetY = cameraPosition.y % gridSize
    
    for (x in 0 until (size.width / gridSize + 2).toInt()) {
        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = Offset(x * gridSize - offsetX, 0f),
            end = Offset(x * gridSize - offsetX, size.height),
            strokeWidth = 1f
        )
    }
    
    for (y in 0 until (size.height / gridSize + 2).toInt()) {
        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = Offset(0f, y * gridSize - offsetY),
            end = Offset(size.width, y * gridSize - offsetY),
            strokeWidth = 1f
        )
    }
    
    // Draw world objects
    worldObjects.forEach { obj ->
        val screenPos = Offset(
            obj.position.x + cameraPosition.x,
            obj.position.y + cameraPosition.y
        )
        
        // Only draw if object is visible on screen
        if (screenPos.x > -50f && screenPos.x < size.width + 50f &&
            screenPos.y > -50f && screenPos.y < size.height + 50f) {
            
            val objectSize = 20f * cameraZoom
            
            when (obj.type) {
                "Avatar" -> {
                    // Draw avatar as circle with outline
                    drawCircle(
                        color = obj.color,
                        radius = objectSize,
                        center = screenPos
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = objectSize,
                        center = screenPos,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                }
                "Sphere" -> {
                    drawCircle(
                        color = obj.color,
                        radius = objectSize * 0.8f,
                        center = screenPos
                    )
                }
                else -> {
                    // Draw as rectangle for cubes and other objects
                    drawRect(
                        color = obj.color,
                        topLeft = Offset(
                            screenPos.x - objectSize * 0.7f,
                            screenPos.y - objectSize * 0.7f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            objectSize * 1.4f,
                            objectSize * 1.4f
                        )
                    )
                }
            }
        }
    }
}

private data class WorldObject(
    val id: String,
    val type: String,
    val position: Offset,
    val color: Color
)