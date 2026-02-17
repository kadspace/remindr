package com.remindr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.material3.MaterialTheme
import com.remindr.app.data.db.DatabaseDriverFactory
import com.remindr.app.data.db.ItemRepository
import com.remindr.app.db.RemindrDatabase
import com.remindr.app.ui.theme.RemindrTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class MainActivity : ComponentActivity() {

    companion object {
        private const val MISSED_REMINDER_LOOKBACK_MS = 24L * 60L * 60L * 1000L
        private var staticLogCallback: ((String) -> Unit)? = null

        fun logFromReceiver(message: String) {
            staticLogCallback?.invoke(message)
        }
    }

    private var notificationTestCallback: ((String) -> Unit)? = null
    private var exactAlarmPermissionCallback: ((String) -> Unit)? = null

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        val log = notificationTestCallback ?: return@registerForActivityResult
        if (isGranted) {
            log("Permission granted")
            log("Please click the button again to send.")
        } else {
            log("Permission denied")
        }
    }

    private val requestExactAlarmPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        val log = exactAlarmPermissionCallback ?: return@registerForActivityResult
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            log("Exact alarms do not require special access on this Android version.")
            return@registerForActivityResult
        }

        val alarmManager = getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        if (alarmManager.canScheduleExactAlarms()) {
            log("Exact alarm access enabled.")
        } else {
            log("Exact alarm access still disabled.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val driverFactory = DatabaseDriverFactory(applicationContext)
        val appVersionLabel = resolveAppVersionLabel()

        val requestMagicAdd = intent?.action == "ACTION_MAGIC_ADD"
        val scheduler = AndroidReminderScheduler(this)
        reconcileReminders(scheduler)

        setContent {
            MaterialTheme(RemindrTheme) {
                RemindrApp(
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
                    },
                    onRequestExactAlarmPermission = { logCallback ->
                        staticLogCallback = logCallback
                        requestExactAlarmPermission(logCallback)
                    },
                    appVersionLabel = appVersionLabel,
                )
            }
        }
    }

    private fun resolveAppVersionLabel(): String {
        val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }

        val versionName = packageInfo.versionName ?: "unknown"
        val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        return "$versionName ($versionCode)"
    }

    private fun reconcileReminders(scheduler: AndroidReminderScheduler) {
        lifecycleScope.launch(Dispatchers.IO) {
            val repository = ItemRepository(
                RemindrDatabase(DatabaseDriverFactory(applicationContext).createDriver()),
            )
            val nowMs = System.currentTimeMillis()

            repository.getSchedulableItemsSnapshot().forEach { item ->
                val dueAt = item.time ?: return@forEach
                val dueAtMs = dueAt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

                if (dueAtMs > nowMs) {
                    scheduler.schedule(item)
                    return@forEach
                }

                if (nowMs - dueAtMs > MISSED_REMINDER_LOOKBACK_MS) return@forEach
                if (repository.wasMissedReminderReconciled(item.id, dueAt)) return@forEach

                val reminderIntent = android.content.Intent(applicationContext, ReminderReceiver::class.java).apply {
                    putExtra("ITEM_ID", item.id)
                    putExtra("ITEM_TEXT", item.text)
                    putExtra("NAG_ENABLED", item.nagEnabled)
                }
                applicationContext.sendBroadcast(reminderIntent)
                repository.markMissedReminderReconciled(item.id, dueAt)
                logFromReceiver("Recovered missed reminder: ${item.id}")
            }
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

    private fun requestExactAlarmPermission(log: (String) -> Unit) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            log("Exact alarms do not require special access on this Android version.")
            return
        }

        val alarmManager = getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        if (alarmManager.canScheduleExactAlarms()) {
            log("Exact alarm access already enabled.")
            return
        }

        exactAlarmPermissionCallback = log
        val requestIntent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = android.net.Uri.parse("package:$packageName")
        }

        try {
            log("Opening exact alarm settings...")
            requestExactAlarmPermissionLauncher.launch(requestIntent)
        } catch (_: android.content.ActivityNotFoundException) {
            val appDetailsIntent = android.content.Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.fromParts("package", packageName, null),
            )
            requestExactAlarmPermissionLauncher.launch(appDetailsIntent)
        }
    }

    private fun sendRichNotification(log: (String) -> Unit) {
        log("Preparing RICH notification...")
        createNotificationChannel(log)

        val snoozeIntent = android.content.Intent(this, NotificationActionReceiver::class.java).apply {
            action = "ACTION_SNOOZE"
            putExtra("IS_TEST_NOTIFICATION", true)
            putExtra("NOTIFICATION_ID", 2)
        }
        val snoozePendingIntent = android.app.PendingIntent.getBroadcast(
            this, 101, snoozeIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        val doneIntent = android.content.Intent(this, NotificationActionReceiver::class.java).apply {
            action = "ACTION_DONE"
            putExtra("IS_TEST_NOTIFICATION", true)
            putExtra("NOTIFICATION_ID", 2)
        }
        val donePendingIntent = android.app.PendingIntent.getBroadcast(
            this, 102, doneIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        val deleteIntent = android.content.Intent(this, NotificationActionReceiver::class.java).apply {
            action = "ACTION_DELETE"
            putExtra("IS_TEST_NOTIFICATION", true)
            putExtra("NOTIFICATION_ID", 2)
        }
        val deletePendingIntent = android.app.PendingIntent.getBroadcast(
            this, 103, deleteIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = androidx.core.app.NotificationCompat.Builder(this, "remindr_debug_channel")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("HIGH - Rich Notification Test")
            .setContentText("Expand me to see more content!")
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle()
                .bigText("This is a rich notification demonstrating expanded text.\n\nTry clicking 'Snooze' or 'Done'."))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setOngoing(true)
            .setDeleteIntent(deletePendingIntent)
            .setColor(android.graphics.Color.CYAN)
            .setColorized(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Snooze", snoozePendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Done", donePendingIntent)

        try {
            val notificationManager = androidx.core.app.NotificationManagerCompat.from(this)
            log("Sending rich notification id 2...")
            if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                log("Permission missing at send time!")
                return
            }
            notificationManager.notify(2, builder.build())
            log("Rich Notification sent successfully.")
        } catch (e: Exception) {
            log("Error sending: ${e.message}")
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
            if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                log("Permission missing at send time!")
                return
            }
            notificationManager.notify(1, builder.build())
            log("Notification sent successfully.")
        } catch (e: Exception) {
            log("Error sending: ${e.message}")
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
