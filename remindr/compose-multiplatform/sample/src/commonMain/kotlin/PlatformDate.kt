package com.kizitonwose.remindr.compose.multiplatform.sample

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

expect fun getToday(): LocalDate
expect fun getCurrentDateTime(): LocalDateTime
