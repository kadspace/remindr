package com.kizitonwose.calendar.compose.multiplatform.sample

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kizitonwose.calendar.compose.multiplatform.MainActivity
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.TimeZone
// import kotlinx.datetime.DatePeriod // Unused
// import kotlinx.datetime.plus // Unused
// import kotlinx.datetime.Clock // Unused

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getLongExtra("NOTE_ID", -1)
        if (noteId == -1L) return

        // Robust DB Check
        val driverFactory = com.kizitonwose.calendar.compose.multiplatform.sample.DatabaseDriverFactory(context)
        val database = com.kizitonwose.calendar.sample.db.RemindrDatabase(driverFactory.createDriver())
        val dbHelper = com.kizitonwose.calendar.compose.multiplatform.sample.NoteDbHelper(database)
        val scheduler = AndroidReminderScheduler(context)

        val note = dbHelper.getNoteById(noteId) ?: return

        if (note.isCompleted) {
            // Task is done. Do not notify.
            scheduler.cancel(note)
            return
        }

        showNotification(context, note)

        if (note.nagEnabled) {
             val javaNow = java.time.LocalDateTime.now()
             val javaNext = javaNow.plusDays(1)
             
             // Convert to Kotlin LocalDateTime
             val nextNagLocal = kotlinx.datetime.LocalDateTime(
                 year = javaNext.year,
                 monthNumber = javaNext.monthValue,
                 dayOfMonth = javaNext.dayOfMonth,
                 hour = javaNext.hour,
                 minute = javaNext.minute,
                 second = javaNext.second,
                 nanosecond = javaNext.nano
             )
             
             val nagNote = note.copy(
                 time = nextNagLocal,
                 reminderOffsets = listOf(0L)
             )
             
             scheduler.schedule(nagNote)
        }
    }

    private fun showNotification(context: Context, note: com.kizitonwose.calendar.compose.multiplatform.sample.CalendarNote) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "remindr_channel" // Could use different channels for high severity

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = when(note.severity) {
                com.kizitonwose.calendar.compose.multiplatform.sample.Severity.HIGH -> NotificationManager.IMPORTANCE_HIGH
                com.kizitonwose.calendar.compose.multiplatform.sample.Severity.LOW -> NotificationManager.IMPORTANCE_LOW
                else -> NotificationManager.IMPORTANCE_DEFAULT
            }
            
            val channel = NotificationChannel(
                channelId,
                "Reminders",
                importance
            )
            notificationManager.createNotificationChannel(channel)
        }

        val contentIntent = Intent(context, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            note.id.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconId = context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName)
        
        // Severity Color Logic
        val color = when(note.severity) {
            com.kizitonwose.calendar.compose.multiplatform.sample.Severity.HIGH -> android.graphics.Color.RED
            com.kizitonwose.calendar.compose.multiplatform.sample.Severity.MEDIUM -> android.graphics.Color.YELLOW
            com.kizitonwose.calendar.compose.multiplatform.sample.Severity.LOW -> android.graphics.Color.BLUE
        }
        
        // Actions
        // TODO: Implement BroadcastReceivers for these Actions
        // val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply { putExtra("NOTE_ID", note.id) }
        // val doneIntent = Intent(context, DoneReceiver::class.java).apply { putExtra("NOTE_ID", note.id) }
        
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(if (iconId != 0) iconId else android.R.drawable.ic_popup_reminder)
            .setContentTitle(note.title)
            .setContentText(note.description ?: note.time.toString())
            .setStyle(NotificationCompat.BigTextStyle().bigText(note.description ?: ""))
            .setPriority(NotificationCompat.PRIORITY_HIGH) 
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setColor(color)
            .setColorized(true)
            .setLights(color, 500, 2000)
            // .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Snooze", snoozePendingIntent)
            // .addAction(android.R.drawable.checkbox_on_background, "Done", donePendingIntent)

        notificationManager.notify(note.id.toInt(), notificationBuilder.build())
    }
}
