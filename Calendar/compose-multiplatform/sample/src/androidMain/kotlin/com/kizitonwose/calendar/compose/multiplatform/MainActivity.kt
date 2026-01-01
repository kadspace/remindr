package com.kizitonwose.calendar.compose.multiplatform

import App
import com.kizitonwose.calendar.compose.multiplatform.sample.DatabaseDriverFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val driverFactory = DatabaseDriverFactory(applicationContext)
        
        val requestMagicAdd = intent?.action == "ACTION_MAGIC_ADD"
        
        setContent {
            App(driverFactory, requestMagicAdd)
        }
    }
}

/*
@Preview
@Composable
fun AppAndroidPreview() {
    // App() // Requires Factory now
}
*/
