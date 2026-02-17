package com.remindr.app.domain

import com.remindr.app.data.model.Item

interface ReminderScheduler {
    fun schedule(item: Item)
    fun cancel(item: Item)
}
