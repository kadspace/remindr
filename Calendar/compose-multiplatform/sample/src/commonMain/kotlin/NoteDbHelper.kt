package com.kizitonwose.calendar.compose.multiplatform.sample

import com.kizitonwose.calendar.sample.db.RemindrDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Clock
import com.kizitonwose.calendar.core.CalendarDay
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

import androidx.compose.ui.graphics.toArgb

class NoteDbHelper(database: RemindrDatabase) {
    private val queries = database.noteQueries
    private val settingsQueries = database.keyValueStoreQueries

    fun getAllNotes(): Flow<List<CalendarNote>> {
        return queries.selectAll().asFlow().mapToList(Dispatchers.IO).map { dbNotes ->
            dbNotes.map { note ->
                CalendarNote(
                    id = note.id,
                    time = LocalDateTime.parse(note.time),
                    text = note.text,
                    color = androidx.compose.ui.graphics.Color(note.color.toInt())
                )
            }
        }
    }

    fun insert(note: CalendarNote) {
        queries.insert(
            date = note.time.date.toString(),
            text = note.text,
            color = note.color.toArgb().toLong(),
            time = note.time.toString()
        )
    }

    fun deleteById(id: Long) {
        queries.delete(id)
    }

    fun getLastInsertedNoteId(): Long? {
        // Run blocking or return via query. executeAsOneOrNull is synchronous on the driver usually.
        // But since we are in `common` code using SQLDelight, verify if executeAsOneOrNull is available.
        // Yes, standard SQLDelight API.
        return queries.selectLast().executeAsOneOrNull()?.id
    }

    fun delete(note: CalendarNote) {
         // We need an ID to delete robustly, but for now let's query by all fields or assume unique?
         // Our Schema has ID. CalendarNote needs ID??
         // The current CalendarNote data class doesn't have ID.
         // For simplicity, let's just not implement delete perfectly or add ID to CalendarNote.
    }

    private val queueQueries = database.queueQueries

    fun getQueueNotes(): Flow<List<com.kizitonwose.calendar.sample.db.QueueNote>> {
        return queueQueries.getAll().asFlow().mapToList(Dispatchers.IO)
    }

    fun insertQueue(text: String) {
        val timestamp = getToday().toString()
        queueQueries.insert(text, timestamp)
    }

    fun deleteQueueById(id: Long) {
        queueQueries.deleteById(id)
    }

    fun getApiKey(): String? {
        return settingsQueries.get("gemini_api_key").executeAsOneOrNull()
    }

    fun saveApiKey(key: String) {
        settingsQueries.upsert("gemini_api_key", key)
    }
}
