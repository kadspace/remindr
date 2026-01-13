package com.kizitonwose.calendar.compose.multiplatform

import App
import com.kizitonwose.calendar.compose.multiplatform.sample.DatabaseDriverFactory
import com.kizitonwose.calendar.compose.multiplatform.sample.AndroidReminderScheduler
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {

    companion object {
        private var staticLogCallback: ((String) -> Unit)? = null

        fun logFromReceiver(message: String) {
            staticLogCallback?.invoke(message)
        }
    }

    private var notificationTestCallback: ((String) -> Unit)? = null

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        val log = notificationTestCallback ?: return@registerForActivityResult
        if (isGranted) {
            log("Permission granted")
            // We don't know which one triggered it, default to simple for now or track state.
            // For prototype, we assume user clicks button again if permission was justified.
            log("Please click the button again to send.")
        } else {
            log("Permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val driverFactory = DatabaseDriverFactory(applicationContext)
        
        val requestMagicAdd = intent?.action == "ACTION_MAGIC_ADD"
        val scheduler = AndroidReminderScheduler(this)
        
        setContent {
            App(
                driverFactory = driverFactory, 
                requestMagicAdd = requestMagicAdd, 
                scheduler = scheduler,
                onRequestNotificationTest = { logCallback ->
                    notificationTestCallback = logCallback
                    staticLogCallback = logCallback
                    checkPermissionAndSend(logCallback, isRich = false)
                },
                onRequestRichNotificationTest = { logCallback ->
                    notificationTestCallback = logCallback
                    staticLogCallback = logCallback
                    checkPermissionAndSend(logCallback, isRich = true)
                }
            )
        }
    }

    private fun checkPermissionAndSend(log: (String) -> Unit, isRich: Boolean) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
             if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                 if (isRich) sendRichNotification(log) else sendNotification(log)
             } else {
                 log("Requesting permission...")
                 requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
             }
        } else {
             if (isRich) sendRichNotification(log) else sendNotification(log)
        }
    }

    private fun sendRichNotification(log: (String) -> Unit) {
        log("Preparing RICH notification...")
        createNotificationChannel(log)
        
        val snoozeIntent = android.content.Intent(this, com.kizitonwose.calendar.compose.multiplatform.sample.NotificationActionReceiver::class.java).apply {
            action = "ACTION_SNOOZE"
            putExtra("NOTE_ID", 2)
        }
        val snoozePendingIntent = android.app.PendingIntent.getBroadcast(
            this, 101, snoozeIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = android.content.Intent(this, com.kizitonwose.calendar.compose.multiplatform.sample.NotificationActionReceiver::class.java).apply {
            action = "ACTION_DONE"
            putExtra("NOTE_ID", 2)
        }
        val donePendingIntent = android.app.PendingIntent.getBroadcast(
            this, 102, doneIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val deleteIntent = android.content.Intent(this, com.kizitonwose.calendar.compose.multiplatform.sample.NotificationActionReceiver::class.java).apply {
            action = "ACTION_DELETE"
            putExtra("NOTE_ID", 2)
        }
        val deletePendingIntent = android.app.PendingIntent.getBroadcast(
            this, 103, deleteIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = androidx.core.app.NotificationCompat.Builder(this, "remindr_debug_channel")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("🔴 HIGH • Rich Notification Test")
            .setContentText("Expand me to see more content!")
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle()
                .bigText("🔴 This is a rich notification demonstrating expanded text.\n\nSince Android 12+, background colors are restricted, so we use Emojis! 🎨\n\nTry clicking 'Snooze' 💤 or 'Done' ✅."))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false) // Do not dismiss on click
            .setOngoing(true)     // Persistent
            .setDeleteIntent(deletePendingIntent) // Track dismissal
            .setColor(android.graphics.Color.CYAN)
            .setColorized(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Snooze", snoozePendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Done", donePendingIntent)

        try {
            val notificationManager = androidx.core.app.NotificationManagerCompat.from(this)
            log("Sending rich notification id 2...")
            if (androidx.core.app.ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
            ) {
                log("Permission missing at send time!")
                return
            }
            notificationManager.notify(2, builder.build())
            log("Rich Notification sent successfully.")
        } catch (e: Exception) {
            log("Error sending: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun sendNotification(log: (String) -> Unit) {
        log("Preparing notification...")
        createNotificationChannel(log)
        val builder = androidx.core.app.NotificationCompat.Builder(this, "remindr_debug_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Remindr Test")
            .setContentText("This is a test notification from Settings.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            val notificationManager = androidx.core.app.NotificationManagerCompat.from(this)
            log("Sending notification id 1...")
            
            if (androidx.core.app.ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
            ) {
                log("Permission missing at send time!")
                return
            }
            notificationManager.notify(1, builder.build())
            log("Notification sent successfully.")
        } catch (e: Exception) {
            log("Error sending: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel(log: (String) -> Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Debug Channel"
            val descriptionText = "Channel for debug notifications"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel("remindr_debug_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: android.app.NotificationManager =
                getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
            log("Notification channel ensured.")
        }
    }
}

/*
@Preview
@Composable
fun AppAndroidPreview() {
    // App() // Requires Factory now
}
*/
