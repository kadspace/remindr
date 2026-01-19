package com.kizitonwose.remindr.compose.multiplatform.sample

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.kizitonwose.remindr.compose.multiplatform.sample.ReminderReceiver
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import android.os.Build

class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(note: CalendarNote) {
        val offsets = if (note.reminderOffsets.isEmpty()) listOf(0L) else note.reminderOffsets
        
        offsets.forEach { offsetMinutes ->
             val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("NOTE_ID", note.id)
                putExtra("NOTE_TEXT", note.text)
                putExtra("NAG_ENABLED", note.nagEnabled)
            }
    
            // Unique requestCode per note AND offset
            // We can compound the ID: noteId * 1000 + offset?
            // Or hash. noteId is Long, requestCode is Int.
            // Assumption: noteId is small enough or we accept collision risk?
            // Better: Hash string "$noteId-$offsetMinutes"
            val requestCode = "${note.id}-$offsetMinutes".hashCode()
    
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
    
            val triggerTime = note.time.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            val actualTriggerTime = triggerTime - (offsetMinutes * 60 * 1000)
    
            if (actualTriggerTime > System.currentTimeMillis()) {
                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                         alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            actualTriggerTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            actualTriggerTime,
                            pendingIntent
                        )
                    }
                } else {
                     alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        actualTriggerTime,
                        pendingIntent
                    )
                }
            }
        }
    }

    override fun cancel(note: CalendarNote) {
        // Cancel all possible offsets. Ideally we track them, but for now we regenerate logic.
        // If we don't know what offsets were scheduled, we might miss some if the note changed.
        // Best effort: Cancel assuming current offsets.
        val offsets = if (note.reminderOffsets.isEmpty()) listOf(0L) else note.reminderOffsets
        offsets.forEach { offsetMinutes ->
            val requestCode = "${note.id}-$offsetMinutes".hashCode()
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
