package com.kizitonwose.remindr.compose.multiplatform.sample

import androidx.compose.runtime.Composable

@Composable
expect fun rememberSpeechRecognizer(onResult: (String) -> Unit): () -> Unit
