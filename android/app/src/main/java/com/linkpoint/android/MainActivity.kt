package com.linkpoint.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.linkpoint.android.ui.theme.LinkpointTheme
import com.linkpoint.android.ui.LinkpointApp

/**
 * Main Activity for the Linkpoint Android Virtual World Viewer
 * 
 * This activity serves as the entry point for the Android version of Linkpoint,
 * a modern Kotlin virtual world viewer based on SecondLife, Firestorm, and RLV viewers.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            LinkpointTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LinkpointApp()
                }
            }
        }
    }
}