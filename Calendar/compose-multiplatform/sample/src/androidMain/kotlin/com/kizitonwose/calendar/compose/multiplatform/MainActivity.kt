package com.kizitonwose.calendar.compose.multiplatform

import App
import com.kizitonwose.calendar.compose.multiplatform.sample.DatabaseDriverFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val driverFactory = DatabaseDriverFactory(applicationContext)
        setContent {
            App(driverFactory)
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
