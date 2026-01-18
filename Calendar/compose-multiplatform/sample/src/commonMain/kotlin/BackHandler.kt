package com.kizitonwose.calendar.compose.multiplatform.sample

import androidx.compose.runtime.Composable

@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
