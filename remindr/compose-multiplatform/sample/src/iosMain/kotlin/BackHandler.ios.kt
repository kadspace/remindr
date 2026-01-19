package com.kizitonwose.remindr.compose.multiplatform.sample

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op for iOS
}
