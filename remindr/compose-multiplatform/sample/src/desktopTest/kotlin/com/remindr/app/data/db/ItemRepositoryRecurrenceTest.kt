package com.remindr.app.data.db

import androidx.compose.ui.graphics.Color
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.remindr.app.data.model.Item
import com.remindr.app.data.model.ItemStatus
import com.remindr.app.db.RemindrDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.fail
import kotlinx.datetime.LocalDateTime

class ItemRepositoryRecurrenceTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: ItemRepository

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        RemindrDatabase.Schema.create(driver)
        repository = ItemRepository(RemindrDatabase(driver))
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun completingThenUndoingRecurringReminderReopensOriginalOccurrence() {
        val createdAt = LocalDateTime(2026, 2, 1, 9, 0)
        val dueAt = LocalDateTime(2026, 2, 10, 9, 0)
        repository.insert(
            Item(
                title = "Discover Card Payment",
                description = "Pay statement balance.",
                time = dueAt,
                color = Color(0xFF8A8A8A),
                status = ItemStatus.PENDING,
                recurrenceType = "MONTHLY",
                recurrenceInterval = 1,
                recurrenceEndMode = "NEVER",
                createdAt = createdAt,
                reminderOffsets = listOf(0L),
            ),
        )

        val originalId = repository.getLastInsertedItemId() ?: fail("Expected inserted occurrence id")
        val originalItem = repository.getItemById(originalId) ?: fail("Expected inserted occurrence")
        val seriesId = originalItem.parentId ?: fail("Expected series id")
        assertEquals(ItemStatus.PENDING, originalItem.status)

        repository.updateStatus(originalId, ItemStatus.COMPLETED)

        val completedOriginal = repository.getItemById(originalId) ?: fail("Expected original occurrence")
        assertEquals(ItemStatus.COMPLETED, completedOriginal.status)

        val generatedNext = repository.getNextOpenItemForSeries(seriesId) ?: fail("Expected generated next occurrence")
        assertNotEquals(originalId, generatedNext.id)

        repository.updateStatus(originalId, ItemStatus.PENDING)

        val reopenedOriginal = repository.getItemById(originalId) ?: fail("Expected reopened original occurrence")
        assertEquals(ItemStatus.PENDING, reopenedOriginal.status)

        val nextOpenAfterUndo = repository.getNextOpenItemForSeries(seriesId) ?: fail("Expected open occurrence after undo")
        assertEquals(
            expected = originalId,
            actual = nextOpenAfterUndo.id,
            message = "Undo should reopen original occurrence and remove generated next occurrence.",
        )
    }
}
