package com.remindr.app.data.db

import com.remindr.app.data.model.Group
import com.remindr.app.data.model.Item
import com.remindr.app.data.model.ItemStatus
import com.remindr.app.data.model.ItemType
import com.remindr.app.data.model.Severity
import com.remindr.app.db.RemindrDatabase
import com.remindr.app.util.getCurrentDateTime
import com.remindr.app.util.getToday
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

class ItemRepository(database: RemindrDatabase) {
    private val itemQueries = database.itemTableQueries
    private val groupQueries = database.groupTableQueries
    private val settingsQueries = database.keyValueStoreQueries

    // === Items ===

    fun getAllItems(): Flow<List<Item>> {
        return itemQueries.selectAll().asFlow().mapToList(Dispatchers.IO).map { rows ->
            rows.map { it.toItem() }
        }
    }

    fun getItemsByDate(targetDate: String): Flow<List<Item>> {
        return itemQueries.selectByDate(targetDate).asFlow().mapToList(Dispatchers.IO).map { rows ->
            rows.map { it.toItem() }
        }
    }

    fun getItemsByGroupId(groupId: Long): Flow<List<Item>> {
        return itemQueries.selectByGroupId(groupId).asFlow().mapToList(Dispatchers.IO).map { rows ->
            rows.map { it.toItem() }
        }
    }

    fun getItemsByStatus(status: String): Flow<List<Item>> {
        return itemQueries.selectByStatus(status).asFlow().mapToList(Dispatchers.IO).map { rows ->
            rows.map { it.toItem() }
        }
    }

    fun getItemsByType(type: String): Flow<List<Item>> {
        return itemQueries.selectByType(type).asFlow().mapToList(Dispatchers.IO).map { rows ->
            rows.map { it.toItem() }
        }
    }

    fun getUpcomingItems(startDate: String, endDate: String): Flow<List<Item>> {
        return getAllItems().map { items ->
            items.filter { item ->
                val date = item.time?.date?.toString()
                date != null && date >= startDate && date <= endDate
                    && item.status != ItemStatus.COMPLETED && item.status != ItemStatus.ARCHIVED
            }
        }
    }

    fun getOverdueItems(today: String): Flow<List<Item>> {
        return getAllItems().map { items ->
            items.filter { item ->
                val date = item.time?.date?.toString()
                date != null && date < today
                    && item.status != ItemStatus.COMPLETED && item.status != ItemStatus.ARCHIVED
            }
        }
    }

    fun getItemsWithAmounts(): Flow<List<Item>> {
        return getAllItems().map { items ->
            items.filter { it.amount != null && it.status != ItemStatus.ARCHIVED }
        }
    }

    fun getItemById(id: Long): Item? {
        val row = itemQueries.selectById(id).executeAsOneOrNull() ?: return null
        return row.toItem()
    }

    fun insert(item: Item) {
        itemQueries.insert(
            title = item.title,
            description = item.description,
            time = item.time?.toString(),
            date = item.time?.date?.toString(),
            end_date = item.endDate?.toString(),
            color = item.color.toArgb().toLong(),
            severity = item.severity.name,
            type = item.type.name,
            status = item.status.name,
            group_id = item.groupId,
            parent_id = item.parentId,
            amount = item.amount,
            recurrence_type = item.recurrenceType,
            recurrence_end_date = item.recurrenceEndDate?.toString(),
            recurrence_rule = item.recurrenceRule,
            nag_enabled = if (item.nagEnabled) 1L else 0L,
            last_completed_at = item.lastCompletedAt?.toString(),
            snoozed_until = item.snoozedUntil?.toString(),
            reminder_offsets = item.reminderOffsets.joinToString(","),
            created_at = item.createdAt.toString(),
        )
    }

    fun update(item: Item) {
        itemQueries.update(
            title = item.title,
            description = item.description,
            time = item.time?.toString(),
            date = item.time?.date?.toString(),
            end_date = item.endDate?.toString(),
            color = item.color.toArgb().toLong(),
            severity = item.severity.name,
            type = item.type.name,
            status = item.status.name,
            group_id = item.groupId,
            parent_id = item.parentId,
            amount = item.amount,
            recurrence_type = item.recurrenceType,
            recurrence_end_date = item.recurrenceEndDate?.toString(),
            recurrence_rule = item.recurrenceRule,
            nag_enabled = if (item.nagEnabled) 1L else 0L,
            last_completed_at = item.lastCompletedAt?.toString(),
            snoozed_until = item.snoozedUntil?.toString(),
            reminder_offsets = item.reminderOffsets.joinToString(","),
            id = item.id,
        )
    }

    fun updateStatus(id: Long, status: ItemStatus) {
        val now = getCurrentDateTime()
        itemQueries.updateStatus(
            status = status.name,
            last_completed_at = if (status == ItemStatus.COMPLETED) now.toString() else null,
            id = id,
        )
    }

    fun deleteById(id: Long) {
        itemQueries.delete(id)
    }

    fun getLastInsertedItemId(): Long? {
        return itemQueries.selectLast().executeAsOneOrNull()?.id
    }

    // === Groups ===

    fun getAllGroups(): Flow<List<Group>> {
        return groupQueries.selectAll().asFlow().mapToList(Dispatchers.IO).map { rows ->
            rows.map { it.toGroup() }
        }
    }

    fun getGroupById(id: Long): Group? {
        val row = groupQueries.selectById(id).executeAsOneOrNull() ?: return null
        return row.toGroup()
    }

    fun getGroupByName(name: String): Group? {
        val row = groupQueries.selectByName(name).executeAsOneOrNull() ?: return null
        return row.toGroup()
    }

    fun insertGroup(group: Group) {
        groupQueries.insert(
            name = group.name,
            icon = group.icon,
            description = group.description,
            created_at = group.createdAt.toString(),
            updated_at = group.updatedAt.toString(),
        )
    }

    fun updateGroup(group: Group) {
        groupQueries.update(
            name = group.name,
            icon = group.icon,
            description = group.description,
            updated_at = group.updatedAt.toString(),
            id = group.id,
        )
    }

    fun deleteGroup(id: Long) {
        groupQueries.delete(id)
    }

    fun getLastInsertedGroupId(): Long? {
        return groupQueries.selectLast().executeAsOneOrNull()?.id
    }

    // === Settings ===

    fun getApiKey(): String? {
        return settingsQueries.get("gemini_api_key").executeAsOneOrNull()
    }

    fun saveApiKey(key: String) {
        settingsQueries.upsert("gemini_api_key", key)
    }

    fun getEventTypeLabels(): List<String> {
        val saved = settingsQueries.get("event_type_labels").executeAsOneOrNull()
        return if (saved != null) {
            saved.split("|||").filter { it.isNotBlank() }
        } else {
            listOf("Work", "Critical", "Personal", "Health", "Misc")
        }
    }

    fun saveEventTypeLabels(labels: List<String>) {
        val joined = labels.joinToString("|||")
        settingsQueries.upsert("event_type_labels", joined)
    }

    // === Mappers ===

    private fun com.remindr.app.db.Item.toItem(): Item {
        return Item(
            id = id,
            title = title,
            description = description,
            time = time?.let { LocalDateTime.parse(it) },
            endDate = end_date?.let { LocalDateTime.parse(it) },
            color = Color(color.toInt()),
            severity = try { Severity.valueOf(severity) } catch (_: Exception) { Severity.MEDIUM },
            type = try { ItemType.valueOf(type) } catch (_: Exception) { ItemType.TASK },
            status = try { ItemStatus.valueOf(status) } catch (_: Exception) { ItemStatus.PENDING },
            groupId = group_id,
            parentId = parent_id,
            amount = amount,
            recurrenceType = recurrence_type,
            recurrenceEndDate = recurrence_end_date?.let { LocalDateTime.parse(it) },
            recurrenceRule = recurrence_rule,
            nagEnabled = nag_enabled == 1L,
            lastCompletedAt = last_completed_at?.let { LocalDateTime.parse(it) },
            snoozedUntil = snoozed_until?.let { LocalDateTime.parse(it) },
            reminderOffsets = reminder_offsets?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList(),
            createdAt = LocalDateTime.parse(created_at),
        )
    }

    private fun com.remindr.app.db.GroupTable.toGroup(): Group {
        return Group(
            id = id,
            name = name,
            icon = icon,
            description = description,
            createdAt = LocalDateTime.parse(created_at),
            updatedAt = LocalDateTime.parse(updated_at),
        )
    }
}
