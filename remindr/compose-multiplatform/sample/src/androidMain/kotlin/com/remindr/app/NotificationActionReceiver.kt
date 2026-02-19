package com.remindr.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.remindr.app.data.db.DatabaseDriverFactory
import com.remindr.app.data.db.ItemRepository
import com.remindr.app.data.model.ItemStatus
import com.remindr.app.db.RemindrDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        val itemId = intent.getLongExtra("ITEM_ID", -1L).let { id ->
            if (id != -1L) id else intent.getIntExtra("ITEM_ID", -1).toLong()
        }
        val isTestNotification = intent.getBooleanExtra("IS_TEST_NOTIFICATION", false)
        val notificationId = intent.getIntExtra(
            "NOTIFICATION_ID",
            if (itemId != -1L) itemId.toInt() else -1,
        )
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (isTestNotification) {
                    if (notificationId != -1) {
                        NotificationManagerCompat.from(appContext).cancel(notificationId)
                    }
                    MainActivity.logFromReceiver("Test action received: $action")
                    return@launch
                }

                if (itemId == -1L) return@launch

                val repository = ItemRepository(
                    RemindrDatabase(
                        DatabaseDriverFactory(appContext).createDriver(),
                    ),
                )
                val scheduler = AndroidReminderScheduler(appContext)
                val item = repository.getItemById(itemId)

                when (action) {
                    "ACTION_DONE" -> {
                        if (item != null) {
                            repository.updateStatus(itemId, ItemStatus.COMPLETED)
                            scheduler.cancel(item)
                            item.parentId?.let { seriesId ->
                                repository.getNextOpenItemForSeries(seriesId)?.let { nextItem ->
                                    scheduler.schedule(nextItem)
                                }
                            }
                            MainActivity.logFromReceiver("Marked reminder as done: $itemId")
                        }
                    }
                    "ACTION_DELETE" -> {
                        MainActivity.logFromReceiver("Notification dismissed: $itemId")
                    }
                    else -> return@launch
                }

                if (notificationId != -1) {
                    NotificationManagerCompat.from(appContext).cancel(notificationId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
