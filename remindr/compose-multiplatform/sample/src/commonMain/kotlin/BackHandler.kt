package com.kizitonwose.remindr.compose.multiplatform.sample

import androidx.compose.runtime.Composable

@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
