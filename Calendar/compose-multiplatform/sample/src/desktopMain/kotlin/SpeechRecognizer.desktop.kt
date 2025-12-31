package com.kizitonwose.calendar.compose.multiplatform.sample

import androidx.compose.runtime.Composable

@Composable
actual fun rememberSpeechRecognizer(onResult: (String) -> Unit): () -> Unit {
    return {
        // No-op for desktop
        println("Speech recognition not supported on Desktop")
    }
}
