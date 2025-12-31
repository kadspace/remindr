package com.kizitonwose.calendar.compose.multiplatform.sample

import androidx.compose.runtime.Composable

@Composable
actual fun rememberSpeechRecognizer(onResult: (String) -> Unit): () -> Unit {
    return {
        // No-op for Wasm
        println("Speech recognition not supported on Wasm")
    }
}
