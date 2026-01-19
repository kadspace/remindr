package com.kizitonwose.remindr.compose

import androidx.compose.runtime.Immutable
import com.kizitonwose.remindr.core.OutDateStyle
import java.time.DayOfWeek

@Immutable
internal data class CalendarInfo(
    val indexCount: Int,
    private val firstDayOfWeek: DayOfWeek? = null,
    private val outDateStyle: OutDateStyle? = null,
)
