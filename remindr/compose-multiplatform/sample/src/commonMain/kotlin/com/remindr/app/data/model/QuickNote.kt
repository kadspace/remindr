package com.remindr.app.data.model

import com.remindr.app.util.formatTime12
import com.remindr.app.util.toUsDateString
import kotlinx.datetime.LocalDateTime

enum class QuickNoteState {
    ACTIVE,
    ARCHIVED,
    DELETED,
}

data class QuickNote(
    val id: Long = -1L,
    val content: String,
    val state: QuickNoteState = QuickNoteState.ACTIVE,
    val promotedOccurrenceId: Long? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    val isArchived: Boolean
        get() = state == QuickNoteState.ARCHIVED

    val isDeleted: Boolean
        get() = state == QuickNoteState.DELETED

    val summaryLine: String
        get() = "${updatedAt.date.toUsDateString()} at ${formatTime12(updatedAt.time)}"
}
