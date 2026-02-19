package com.remindr.app.data.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import androidx.compose.ui.graphics.Color
import com.remindr.app.data.model.Item
import com.remindr.app.data.model.ItemStatus
import com.remindr.app.data.model.ItemType
import com.remindr.app.data.model.Severity
import com.remindr.app.db.RemindrDatabase
import com.remindr.app.util.getCurrentDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import kotlinx.datetime.plus

class ItemRepository(database: RemindrDatabase) {
    private val seriesQueries = database.reminderSeriesQueries
    private val occurrenceQueries = database.reminderOccurrenceQueries
    private val settingsQueries = database.keyValueStoreQueries

    private data class JoinedOccurrence(
        val occurrenceId: Long,
        val seriesId: Long,
        val dueAt: String,
        val state: String,
        val completedAt: String?,
        val snoozedUntil: String?,
        val reminderOffsetMinutes: Long,
        val occurrenceCreatedAt: String,
        val occurrenceUpdatedAt: String,
        val seriesTitle: String,
        val seriesNotes: String?,
        val seriesStartAt: String,
        val seriesTimezone: String,
        val recurrenceKind: String,
        val recurrenceInterval: Long,
        val byWeekday: Long?,
        val byMonthDay: Long?,
        val endMode: String,
        val endAt: String?,
        val endCount: Long?,
        val generatedCount: Long,
        val reminderOffsets: String,
        val seriesIsActive: Long,
        val seriesArchivedAt: String?,
        val seriesCreatedAt: String,
        val seriesUpdatedAt: String,
    )

    fun getAllItems(): Flow<List<Item>> {
        return selectAllJoinedQuery().asFlow().mapToList(Dispatchers.IO).map { rows ->
            rows.map(::joinedToItem)
        }
    }

    fun getItemById(id: Long): Item? {
        return selectJoinedByOccurrenceIdQuery(id).executeAsOneOrNull()?.let(::joinedToItem)
    }

    fun getSchedulableItemsSnapshot(): List<Item> {
        return selectSchedulableJoinedQuery().executeAsList().map(::joinedToItem)
    }

    fun getNextOpenItemForSeries(seriesId: Long): Item? {
        return selectFirstOpenJoinedBySeriesIdQuery(seriesId).executeAsOneOrNull()?.let(::joinedToItem)
    }

    fun insert(item: Item) {
        val now = getCurrentDateTime()
        val dueAt = item.time ?: now
        val recurrenceKind = normalizedRecurrenceKind(item.recurrenceType)
        val recurrenceInterval = item.recurrenceInterval.coerceAtLeast(1).toLong()
        val byWeekday = if (recurrenceKind == recurrenceWeekly) {
            (dueAt.date.dayOfWeek.ordinal + 1).toLong()
        } else {
            null
        }
        val byMonthDay = if (recurrenceKind == recurrenceMonthly || recurrenceKind == recurrenceYearly) {
            dueAt.date.day.toLong()
        } else {
            null
        }
        val endMode = when {
            recurrenceKind == recurrenceNone -> endModeNever
            item.recurrenceEndMode == endModeAfterCount -> endModeAfterCount
            item.recurrenceEndDate != null -> endModeUntilDate
            else -> endModeNever
        }
        val isActive = if (item.status == ItemStatus.ARCHIVED || item.status == ItemStatus.DELETED) 0L else 1L
        val archivedAt = if (isActive == 0L) now.toString() else null
        val reminderOffsets = normalizeOffsets(item.reminderOffsets).joinToString(",")

        seriesQueries.insert(
            title = item.title,
            notes = item.description,
            start_at = dueAt.toString(),
            timezone = "SYSTEM",
            recurrence_kind = recurrenceKind,
            recurrence_interval = recurrenceInterval,
            by_weekday = byWeekday,
            by_month_day = byMonthDay,
            end_mode = endMode,
            end_at = item.recurrenceEndDate?.toString(),
            end_count = null,
            generated_count = 1L,
            reminder_offsets = reminderOffsets,
            is_active = isActive,
            archived_at = archivedAt,
            created_at = now.toString(),
            updated_at = now.toString(),
        )

        val seriesId = seriesQueries.selectLast().executeAsOneOrNull()?.id ?: return
        val initialState = when (item.status) {
            ItemStatus.COMPLETED -> occurrenceCompleted
            ItemStatus.ARCHIVED -> occurrenceCancelled
            ItemStatus.DELETED -> occurrenceDeleted
            else -> if (item.snoozedUntil != null) occurrenceSnoozed else occurrencePending
        }

        occurrenceQueries.insert(
            series_id = seriesId,
            due_at = dueAt.toString(),
            state = initialState,
            completed_at = if (initialState == occurrenceCompleted) now.toString() else null,
            snoozed_until = item.snoozedUntil?.toString(),
            reminder_offset_minutes = 0L,
            created_at = now.toString(),
            updated_at = now.toString(),
        )
    }

    fun update(item: Item) {
        val current = selectJoinedByOccurrenceIdQuery(item.id).executeAsOneOrNull() ?: return
        val now = getCurrentDateTime()
        val dueAt = item.time ?: LocalDateTime.parse(current.dueAt)
        val recurrenceKind = normalizedRecurrenceKind(item.recurrenceType ?: current.recurrenceKind)
        val recurrenceInterval = item.recurrenceInterval.coerceAtLeast(1).toLong()
        val endMode = when {
            recurrenceKind == recurrenceNone -> endModeNever
            item.recurrenceEndMode == endModeAfterCount -> endModeAfterCount
            item.recurrenceEndDate != null -> endModeUntilDate
            else -> item.recurrenceEndMode.ifBlank { current.endMode }
        }
        val isActive = if (item.status == ItemStatus.ARCHIVED || item.status == ItemStatus.DELETED) 0L else 1L
        val archivedAt = if (isActive == 0L) now.toString() else null
        val byWeekday = if (recurrenceKind == recurrenceWeekly) {
            (dueAt.date.dayOfWeek.ordinal + 1).toLong()
        } else {
            null
        }
        val byMonthDay = if (recurrenceKind == recurrenceMonthly || recurrenceKind == recurrenceYearly) {
            dueAt.date.day.toLong()
        } else {
            null
        }

        seriesQueries.updateCore(
            title = item.title,
            notes = item.description,
            start_at = current.seriesStartAt,
            recurrence_kind = recurrenceKind,
            recurrence_interval = recurrenceInterval,
            by_weekday = byWeekday,
            by_month_day = byMonthDay,
            end_mode = endMode,
            end_at = item.recurrenceEndDate?.toString(),
            end_count = current.endCount,
            reminder_offsets = normalizeOffsets(item.reminderOffsets.ifEmpty { parseOffsets(current.reminderOffsets) }).joinToString(","),
            is_active = isActive,
            archived_at = archivedAt,
            updated_at = now.toString(),
            id = current.seriesId,
        )

        val newState = when {
            item.status == ItemStatus.DELETED -> occurrenceDeleted
            isActive == 0L -> occurrenceCancelled
            item.status == ItemStatus.COMPLETED -> occurrenceCompleted
            item.snoozedUntil != null -> occurrenceSnoozed
            else -> occurrencePending
        }

        occurrenceQueries.updateCore(
            due_at = dueAt.toString(),
            state = newState,
            snoozed_until = item.snoozedUntil?.toString(),
            updated_at = now.toString(),
            id = item.id,
        )
    }

    fun updateStatus(id: Long, status: ItemStatus) {
        val current = selectJoinedByOccurrenceIdQuery(id).executeAsOneOrNull() ?: return
        val now = getCurrentDateTime()

        when (status) {
            ItemStatus.COMPLETED -> {
                occurrenceQueries.markCompleted(
                    completed_at = now.toString(),
                    updated_at = now.toString(),
                    id = id,
                )
                generateNextOccurrenceIfNeeded(current, now)
            }

            ItemStatus.ARCHIVED -> {
                seriesQueries.setActive(
                    is_active = 0L,
                    archived_at = now.toString(),
                    updated_at = now.toString(),
                    id = current.seriesId,
                )
                occurrenceQueries.cancelOpenBySeriesId(
                    updated_at = now.toString(),
                    seriesId = current.seriesId,
                )
            }

            ItemStatus.DELETED -> {
                seriesQueries.setActive(
                    is_active = 0L,
                    archived_at = now.toString(),
                    updated_at = now.toString(),
                    id = current.seriesId,
                )
                occurrenceQueries.markDeletedBySeriesId(
                    updated_at = now.toString(),
                    seriesId = current.seriesId,
                )
            }

            else -> {
                if (
                    status == ItemStatus.PENDING &&
                    current.state == occurrenceCompleted &&
                    current.recurrenceKind != recurrenceNone
                ) {
                    undoCompletedRecurringOccurrence(current = current, now = now)
                    return
                }

                seriesQueries.setActive(
                    is_active = 1L,
                    archived_at = null,
                    updated_at = now.toString(),
                    id = current.seriesId,
                )
                occurrenceQueries.markPending(
                    updated_at = now.toString(),
                    id = id,
                )
            }
        }
    }

    private fun undoCompletedRecurringOccurrence(current: JoinedOccurrence, now: LocalDateTime) {
        val openOccurrence = selectFirstOpenJoinedBySeriesIdQuery(current.seriesId).executeAsOneOrNull()
        val generatedOpenOccurrenceId = openOccurrence
            ?.occurrenceId
            ?.takeIf { openId ->
                openId != current.occurrenceId && openId > current.occurrenceId
            }

        if (generatedOpenOccurrenceId != null) {
            occurrenceQueries.deleteById(generatedOpenOccurrenceId)
            seriesQueries.setGeneratedCount(
                generated_count = (current.generatedCount - 1L).coerceAtLeast(1L),
                updated_at = now.toString(),
                id = current.seriesId,
            )
        }

        seriesQueries.setActive(
            is_active = 1L,
            archived_at = null,
            updated_at = now.toString(),
            id = current.seriesId,
        )
        occurrenceQueries.markPending(
            updated_at = now.toString(),
            id = current.occurrenceId,
        )
    }

    fun archiveById(id: Long) {
        updateStatus(id, ItemStatus.ARCHIVED)
    }

    @Deprecated("Use updateStatus(..., ItemStatus.DELETED) for soft-delete semantics.")
    fun deleteById(id: Long) {
        updateStatus(id, ItemStatus.DELETED)
    }

    fun getLastInsertedItemId(): Long? {
        return occurrenceQueries.selectLast().executeAsOneOrNull()?.id
    }

    fun getApiKey(): String? {
        return settingsQueries.get("gemini_api_key").executeAsOneOrNull()
    }

    fun saveApiKey(key: String) {
        settingsQueries.upsert("gemini_api_key", key)
    }

    fun getSetting(key: String): String? {
        return settingsQueries.get(key).executeAsOneOrNull()
    }

    fun saveSetting(key: String, value: String) {
        settingsQueries.upsert(key, value)
    }

    fun clearAllData() {
        occurrenceQueries.deleteAll()
        seriesQueries.deleteAll()
        settingsQueries.deleteAll()
    }

    fun wasMissedReminderReconciled(itemId: Long, scheduledTime: LocalDateTime): Boolean {
        val key = reminderReconcileKey(itemId, scheduledTime)
        return settingsQueries.get(key).executeAsOneOrNull() == "1"
    }

    fun markMissedReminderReconciled(itemId: Long, scheduledTime: LocalDateTime) {
        val key = reminderReconcileKey(itemId, scheduledTime)
        settingsQueries.upsert(key, "1")
    }

    private fun generateNextOccurrenceIfNeeded(current: JoinedOccurrence, now: LocalDateTime) {
        if (current.recurrenceKind == recurrenceNone || current.seriesIsActive != 1L) return

        if (current.endMode == endModeAfterCount && current.endCount != null && current.generatedCount >= current.endCount) {
            return
        }

        val dueAt = LocalDateTime.parse(current.dueAt)
        val recurrenceInterval = current.recurrenceInterval.coerceAtLeast(1L).toInt()
        val nextDue = nextDueAfter(
            from = dueAt,
            now = now,
            recurrenceKind = current.recurrenceKind,
            recurrenceInterval = recurrenceInterval,
            byWeekday = current.byWeekday?.toInt(),
            byMonthDay = current.byMonthDay?.toInt(),
        ) ?: return

        val endAt = current.endAt?.let(LocalDateTime::parse)
        if (current.endMode == endModeUntilDate && endAt != null && nextDue > endAt) {
            return
        }

        occurrenceQueries.insert(
            series_id = current.seriesId,
            due_at = nextDue.toString(),
            state = occurrencePending,
            completed_at = null,
            snoozed_until = null,
            reminder_offset_minutes = 0L,
            created_at = now.toString(),
            updated_at = now.toString(),
        )
        seriesQueries.setGeneratedCount(
            generated_count = current.generatedCount + 1L,
            updated_at = now.toString(),
            id = current.seriesId,
        )
    }

    private fun nextDueAfter(
        from: LocalDateTime,
        now: LocalDateTime,
        recurrenceKind: String,
        recurrenceInterval: Int,
        byWeekday: Int?,
        byMonthDay: Int?,
    ): LocalDateTime? {
        var next = advance(
            from = from,
            recurrenceKind = recurrenceKind,
            recurrenceInterval = recurrenceInterval,
            byWeekday = byWeekday,
            byMonthDay = byMonthDay,
        ) ?: return null

        while (next <= now) {
            next = advance(
                from = next,
                recurrenceKind = recurrenceKind,
                recurrenceInterval = recurrenceInterval,
                byWeekday = byWeekday,
                byMonthDay = byMonthDay,
            ) ?: return null
        }
        return next
    }

    private fun advance(
        from: LocalDateTime,
        recurrenceKind: String,
        recurrenceInterval: Int,
        byWeekday: Int?,
        byMonthDay: Int?,
    ): LocalDateTime? {
        return when (recurrenceKind) {
            recurrenceDaily -> from.date.plus(DatePeriod(days = recurrenceInterval)).atTime(from.time)
            recurrenceWeekly -> {
                val targetWeekday = (byWeekday ?: (from.date.dayOfWeek.ordinal + 1)).coerceIn(1, 7)
                val base = from.date.plus(DatePeriod(days = 7 * recurrenceInterval))
                val baseIso = base.dayOfWeek.ordinal + 1
                val delta = targetWeekday - baseIso
                base.plus(DatePeriod(days = delta)).atTime(from.time)
            }
            recurrenceMonthly -> {
                val moved = addMonths(from.date, recurrenceInterval)
                val targetDay = (byMonthDay ?: from.date.day).coerceIn(1, 31)
                clampDay(moved.year, moved.monthNumber, targetDay).atTime(from.time)
            }
            recurrenceYearly -> {
                val targetDay = (byMonthDay ?: from.date.day).coerceIn(1, 31)
                val targetYear = from.date.year + recurrenceInterval
                clampDay(targetYear, from.date.monthNumber, targetDay).atTime(from.time)
            }
            else -> null
        }
    }

    private fun addMonths(date: LocalDate, months: Int): LocalDate {
        var year = date.year
        var month = date.monthNumber + months
        while (month > 12) {
            year += 1
            month -= 12
        }
        while (month < 1) {
            year -= 1
            month += 12
        }
        return clampDay(year, month, date.day)
    }

    private fun clampDay(year: Int, month: Int, preferredDay: Int): LocalDate {
        for (day in preferredDay.coerceIn(1, 31) downTo 1) {
            val date = runCatching { LocalDate(year, month, day) }.getOrNull()
            if (date != null) return date
        }
        return LocalDate(year, month, 1)
    }

    private fun joinedToItem(row: JoinedOccurrence): Item {
        val dueAt = LocalDateTime.parse(row.snoozedUntil ?: row.dueAt)
        val status = when {
            row.state == occurrenceDeleted -> ItemStatus.DELETED
            row.seriesIsActive != 1L -> ItemStatus.ARCHIVED
            row.state == occurrenceCompleted -> ItemStatus.COMPLETED
            row.state == occurrenceCancelled -> ItemStatus.ARCHIVED
            else -> ItemStatus.PENDING
        }
        val recurrenceType = row.recurrenceKind.takeUnless { it == recurrenceNone }

        return Item(
            id = row.occurrenceId,
            title = row.seriesTitle,
            description = row.seriesNotes,
            time = dueAt,
            endDate = null,
            color = neutralColor,
            severity = Severity.MEDIUM,
            type = ItemType.TASK,
            status = status,
            groupId = null,
            parentId = row.seriesId,
            amount = null,
            recurrenceType = recurrenceType,
            recurrenceInterval = row.recurrenceInterval.toInt().coerceAtLeast(1),
            recurrenceEndMode = row.endMode,
            recurrenceEndDate = row.endAt?.let { LocalDateTime.parse(it) },
            recurrenceRule = null,
            nagEnabled = false,
            lastCompletedAt = row.completedAt?.let { LocalDateTime.parse(it) },
            snoozedUntil = row.snoozedUntil?.let { LocalDateTime.parse(it) },
            reminderOffsets = parseOffsets(row.reminderOffsets),
            createdAt = LocalDateTime.parse(row.occurrenceCreatedAt),
        )
    }

    private fun selectAllJoinedQuery() = occurrenceQueries.selectAllJoined(
        mapper = ::JoinedOccurrence,
    )

    private fun selectSchedulableJoinedQuery() = occurrenceQueries.selectSchedulableJoined(
        mapper = ::JoinedOccurrence,
    )

    private fun selectJoinedByOccurrenceIdQuery(occurrenceId: Long) =
        occurrenceQueries.selectJoinedByOccurrenceId(
            id = occurrenceId,
            mapper = ::JoinedOccurrence,
        )

    private fun selectFirstOpenJoinedBySeriesIdQuery(seriesId: Long) =
        occurrenceQueries.selectFirstOpenJoinedBySeriesId(
            seriesId = seriesId,
            mapper = ::JoinedOccurrence,
        )

    private fun normalizedRecurrenceKind(recurrenceType: String?): String {
        val raw = recurrenceType?.uppercase()?.trim().orEmpty()
        return when (raw) {
            recurrenceDaily,
            recurrenceWeekly,
            recurrenceMonthly,
            recurrenceYearly,
            -> raw

            else -> recurrenceNone
        }
    }

    private fun parseOffsets(serialized: String): List<Long> {
        return serialized
            .split(",")
            .mapNotNull { value -> value.trim().toLongOrNull() }
            .map { offset -> offset.coerceAtLeast(0L) }
            .ifEmpty { listOf(0L) }
    }

    private fun normalizeOffsets(offsets: List<Long>): List<Long> {
        return offsets
            .ifEmpty { listOf(0L) }
            .map { offset -> offset.coerceAtLeast(0L) }
            .distinct()
    }

    private fun reminderReconcileKey(itemId: Long, scheduledTime: LocalDateTime): String {
        return "reminder_reconciled_${itemId}_${scheduledTime}"
    }

    private companion object {
        val neutralColor = Color(0xFF8A8A8A)

        const val recurrenceNone = "NONE"
        const val recurrenceDaily = "DAILY"
        const val recurrenceWeekly = "WEEKLY"
        const val recurrenceMonthly = "MONTHLY"
        const val recurrenceYearly = "YEARLY"

        const val endModeNever = "NEVER"
        const val endModeUntilDate = "UNTIL_DATE"
        const val endModeAfterCount = "AFTER_COUNT"

        const val occurrencePending = "PENDING"
        const val occurrenceSnoozed = "SNOOZED"
        const val occurrenceCompleted = "COMPLETED"
        const val occurrenceCancelled = "CANCELLED"
        const val occurrenceDeleted = "DELETED"
    }
}
