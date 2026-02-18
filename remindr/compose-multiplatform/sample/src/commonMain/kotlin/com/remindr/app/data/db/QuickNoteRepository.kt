package com.remindr.app.data.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.remindr.app.data.model.QuickNote
import com.remindr.app.db.RemindrDatabase
import com.remindr.app.util.getCurrentDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime

class QuickNoteRepository(database: RemindrDatabase) {
    private val queries = database.quickNoteQueries

    fun getActiveNotes(): Flow<List<QuickNote>> {
        return queries
            .selectAllActive()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows ->
                rows.map { row ->
                    QuickNote(
                        id = row.id,
                        content = row.content,
                        isArchived = row.is_archived == 1L,
                        promotedOccurrenceId = row.promoted_occurrence_id,
                        createdAt = LocalDateTime.parse(row.created_at),
                        updatedAt = LocalDateTime.parse(row.updated_at),
                    )
                }
            }
    }

    fun getById(id: Long): QuickNote? {
        val row = queries.selectById(id).executeAsOneOrNull() ?: return null
        return QuickNote(
            id = row.id,
            content = row.content,
            isArchived = row.is_archived == 1L,
            promotedOccurrenceId = row.promoted_occurrence_id,
            createdAt = LocalDateTime.parse(row.created_at),
            updatedAt = LocalDateTime.parse(row.updated_at),
        )
    }

    fun insert(content: String): Long? {
        val normalized = content.trim()
        if (normalized.isBlank()) return null
        val now = getCurrentDateTime().toString()
        queries.insert(
            content = normalized,
            is_archived = 0L,
            promoted_occurrence_id = null,
            created_at = now,
            updated_at = now,
        )
        return queries.selectLast().executeAsOneOrNull()?.id
    }

    fun updateContent(id: Long, content: String) {
        val normalized = content.trim()
        if (normalized.isBlank()) return
        queries.updateContent(
            content = normalized,
            updated_at = getCurrentDateTime().toString(),
            id = id,
        )
    }

    fun archive(id: Long) {
        queries.setArchived(
            is_archived = 1L,
            updated_at = getCurrentDateTime().toString(),
            id = id,
        )
    }

    fun markPromoted(noteId: Long, occurrenceId: Long) {
        queries.setPromotedOccurrence(
            promoted_occurrence_id = occurrenceId,
            updated_at = getCurrentDateTime().toString(),
            id = noteId,
        )
    }
}

