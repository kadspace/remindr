package com.remindr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.material3.MaterialTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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

        fun logFromReceiver(message: String) {
            // No-op: settings notification test/log plumbing was removed.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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
}
