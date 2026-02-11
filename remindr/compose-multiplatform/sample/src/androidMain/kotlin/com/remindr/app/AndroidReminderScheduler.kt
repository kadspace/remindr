package com.remindr.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.remindr.app.data.model.Item
import com.remindr.app.domain.ReminderScheduler
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(item: Item) {
        if (item.time == null) return
        val offsets = if (item.reminderOffsets.isEmpty()) listOf(0L) else item.reminderOffsets

        offsets.forEach { offsetMinutes ->
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("ITEM_ID", item.id)
                putExtra("ITEM_TEXT", item.text)
                putExtra("NAG_ENABLED", item.nagEnabled)
            }

            val requestCode = "${item.id}-$offsetMinutes".hashCode()

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val triggerTime = item.time.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            val actualTriggerTime = triggerTime - (offsetMinutes * 60 * 1000)

            if (actualTriggerTime > System.currentTimeMillis()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            actualTriggerTime,
                            pendingIntent,
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            actualTriggerTime,
                            pendingIntent,
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        actualTriggerTime,
                        pendingIntent,
                    )
                }
            }
        }
    }

    override fun cancel(item: Item) {
        val offsets = if (item.reminderOffsets.isEmpty()) listOf(0L) else item.reminderOffsets
        offsets.forEach { offsetMinutes ->
            val requestCode = "${item.id}-$offsetMinutes".hashCode()
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
