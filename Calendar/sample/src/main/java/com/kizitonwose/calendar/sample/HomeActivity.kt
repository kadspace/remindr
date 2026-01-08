package com.kizitonwose.calendar.sample

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.updatePadding
import com.kizitonwose.calendar.sample.compose.CalendarComposeActivity
import com.kizitonwose.calendar.sample.databinding.HomeActivityBinding
import com.kizitonwose.calendar.sample.view.CalendarViewActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: HomeActivityBinding

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            log("Permission granted")
            sendNotification()
        } else {
            log("Permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = HomeActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.activityToolbar)
        applyInsets(binding)
        handleClicks(binding)
    }

    private fun handleClicks(binding: HomeActivityBinding) {
        binding.calendarViewSample.setOnClickListener {
            startActivity(Intent(this, CalendarViewActivity::class.java))
        }
        binding.calendarComposeSample.setOnClickListener {
            startActivity(Intent(this, CalendarComposeActivity::class.java))
        }
        binding.testNotificationButton.setOnClickListener {
            checkPermissionAndSend()
        }
    }

    private fun checkPermissionAndSend() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
             if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                 sendNotification()
             } else {
                 log("Requesting permission...")
                 requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
             }
        } else {
             sendNotification()
        }
    }

    private fun sendNotification() {
        log("Preparing notification...")
        createNotificationChannel()
        val builder = androidx.core.app.NotificationCompat.Builder(this, "test_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Test Notification")
            .setContentText("This is a test notification from the app.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            val notificationManager = androidx.core.app.NotificationManagerCompat.from(this)
            log("Sending notification id 1...")
            // Missing permission check for notify is suppressed because we check it in checkPermissionAndSend or it's < Android 13
            if (androidx.core.app.ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
            ) {
                log("Permission missing at send time!")
                return
            }
            notificationManager.notify(1, builder.build())
            log("Notification sent command executed.")
        } catch (e: Exception) {
            log("Error sending notification: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Test Channel"
            val descriptionText = "Channel for test notifications"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel("test_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: android.app.NotificationManager =
                getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
            log("Notification channel created/ensured.")
        }
    }

    private fun log(message: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val fullMessage = "$time: $message\n"
        runOnUiThread {
            binding.logTextView.append(fullMessage)
            // Auto scroll is handled by user scrolling usually, but we could scroll down.
        }
    }

    private fun applyInsets(binding: HomeActivityBinding) {
        ViewCompat.setOnApplyWindowInsetsListener(
            binding.root,
        ) { _, windowInsets ->
            val insets = windowInsets.getInsets(systemBars())
            binding.activityAppBar.updatePadding(top = insets.top)
            binding.root.updatePadding(
                left = insets.left,
                right = insets.right,
                bottom = insets.bottom,
            )
            windowInsets
        }
    }
}
