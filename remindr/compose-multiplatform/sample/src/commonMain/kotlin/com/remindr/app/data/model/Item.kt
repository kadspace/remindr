package com.remindr.app.data.model

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.LocalDateTime

data class Item(
    val id: Long = -1,
    val title: String,
    val description: String? = null,
    val time: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val color: Color,
    val severity: Severity = Severity.MEDIUM,
    val type: ItemType = ItemType.TASK,
    val status: ItemStatus = ItemStatus.PENDING,
    val groupId: Long? = null,
    val parentId: Long? = null,
    val amount: Double? = null,
    val recurrenceType: String? = null,
    val recurrenceEndDate: LocalDateTime? = null,
    val recurrenceRule: String? = null,
    val nagEnabled: Boolean = false,
    val lastCompletedAt: LocalDateTime? = null,
    val snoozedUntil: LocalDateTime? = null,
    val reminderOffsets: List<Long> = emptyList(),
    val createdAt: LocalDateTime,
) {
    val text: String
        get() = if (description.isNullOrBlank()) title else "$title\n$description"

    val isCompleted: Boolean
        get() = status == ItemStatus.COMPLETED
}
