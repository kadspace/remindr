package com.kizitonwose.calendar.compose.multiplatform.sample

import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled, onBack)
}
