package com.remindr.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.remindr.app.data.db.DatabaseDriverFactory
import com.remindr.app.data.db.ItemRepository
import com.remindr.app.db.RemindrDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in supportedActions) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = ItemRepository(
                    RemindrDatabase(
                        DatabaseDriverFactory(appContext).createDriver(),
                    ),
                )
                val scheduler = AndroidReminderScheduler(appContext)
                repository.getSchedulableItemsSnapshot().forEach { item ->
                    scheduler.schedule(item)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val supportedActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
