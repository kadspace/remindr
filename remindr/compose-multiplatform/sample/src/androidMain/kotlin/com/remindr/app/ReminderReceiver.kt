package com.remindr.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.remindr.app.data.db.DatabaseDriverFactory
import com.remindr.app.data.db.ItemRepository
import com.remindr.app.data.model.ItemStatus
import com.remindr.app.data.model.Severity
import com.remindr.app.db.RemindrDatabase

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getLongExtra("ITEM_ID", -1)
        if (itemId == -1L) return

        val driverFactory = DatabaseDriverFactory(context)
        val database = RemindrDatabase(driverFactory.createDriver())
        val repository = ItemRepository(database)
        val scheduler = AndroidReminderScheduler(context)

        val item = repository.getItemById(itemId) ?: return

        if (item.isCompleted) {
            scheduler.cancel(item)
            return
        }

        try {
            showNotification(context, item)
        } catch (_: SecurityException) {
            MainActivity.logFromReceiver("Notification permission missing for item: ${item.id}")
            return
        }

        if (item.nagEnabled) {
            val javaNow = java.time.LocalDateTime.now()
            val javaNext = javaNow.plusDays(1)

            val nextNagLocal = kotlinx.datetime.LocalDateTime(
                year = javaNext.year,
                monthNumber = javaNext.monthValue,
                dayOfMonth = javaNext.dayOfMonth,
                hour = javaNext.hour,
                minute = javaNext.minute,
                second = javaNext.second,
                nanosecond = javaNext.nano,
            )

            val nagItem = item.copy(
                time = nextNagLocal,
                reminderOffsets = listOf(0L),
            )

            scheduler.schedule(nagItem)
        }
    }

    private fun showNotification(context: Context, item: com.remindr.app.data.model.Item) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "remindr_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = when (item.severity) {
                Severity.HIGH -> NotificationManager.IMPORTANCE_HIGH
                Severity.LOW -> NotificationManager.IMPORTANCE_LOW
                else -> NotificationManager.IMPORTANCE_DEFAULT
            }

            val channel = NotificationChannel(channelId, "Reminders", importance)
            notificationManager.createNotificationChannel(channel)
        }

        val contentIntent = Intent(context, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            item.id.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            "${item.id}-done".hashCode(),
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = "ACTION_DONE"
                putExtra("ITEM_ID", item.id)
                putExtra("NOTIFICATION_ID", item.id.toInt())
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            "${item.id}-snooze".hashCode(),
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = "ACTION_SNOOZE"
                putExtra("ITEM_ID", item.id)
                putExtra("NOTIFICATION_ID", item.id.toInt())
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            "${item.id}-delete".hashCode(),
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = "ACTION_DELETE"
                putExtra("ITEM_ID", item.id)
                putExtra("NOTIFICATION_ID", item.id.toInt())
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val iconId = context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName)

        val color = when (item.severity) {
            Severity.HIGH -> android.graphics.Color.RED
            Severity.MEDIUM -> android.graphics.Color.YELLOW
            Severity.LOW -> android.graphics.Color.BLUE
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(if (iconId != 0) iconId else android.R.drawable.ic_popup_reminder)
            .setContentTitle(item.title)
            .setContentText(item.description ?: item.time?.toString() ?: "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.description ?: ""))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setDeleteIntent(deletePendingIntent)
            .setColor(color)
            .setColorized(true)
            .setLights(color, 500, 2000)
            .addAction(android.R.drawable.ic_media_pause, "Snooze 10m", snoozePendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Done", donePendingIntent)

        notificationManager.notify(item.id.toInt(), notificationBuilder.build())
    }
}
