package com.kizitonwose.calendar.compose.multiplatform.sample

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

expect fun getToday(): LocalDate
expect fun getCurrentDateTime(): LocalDateTime
