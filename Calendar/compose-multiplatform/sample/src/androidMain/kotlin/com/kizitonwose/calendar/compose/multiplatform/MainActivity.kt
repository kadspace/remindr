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

    private var notificationTestCallback: ((String) -> Unit)? = null

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        val log = notificationTestCallback ?: return@registerForActivityResult
        if (isGranted) {
            log("Permission granted")
            sendNotification(log)
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
                    checkPermissionAndSend(logCallback)
                }
            )
        }
    }

    private fun checkPermissionAndSend(log: (String) -> Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
             if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                 sendNotification(log)
             } else {
                 log("Requesting permission...")
                 requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
             }
        } else {
             sendNotification(log)
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
