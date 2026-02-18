package com.remindr.app.data.model

import com.remindr.app.util.formatTime12
import kotlinx.datetime.LocalDateTime

data class QuickNote(
    val id: Long = -1L,
    val content: String,
    val isArchived: Boolean = false,
    val promotedOccurrenceId: Long? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    val summaryLine: String
        get() = "${updatedAt.date} at ${formatTime12(updatedAt.time)}"
}

