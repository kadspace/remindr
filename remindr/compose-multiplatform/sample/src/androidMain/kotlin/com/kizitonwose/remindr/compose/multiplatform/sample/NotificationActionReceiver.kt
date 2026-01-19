package com.kizitonwose.remindr.compose.multiplatform.sample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.kizitonwose.remindr.compose.multiplatform.MainActivity
import androidx.core.app.NotificationManagerCompat

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val noteId = intent.getIntExtra("NOTE_ID", -1)
        if (action == "ACTION_DELETE") {
            val log = "Notification Swiped Away (Dismissed)"
            MainActivity.logFromReceiver(log)
            Toast.makeText(context, log, Toast.LENGTH_SHORT).show()
            return
        }

        val message = "Action received: $action"
        
        // concise toast for user feedback
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

        // Dismiss notification on Action Click
        if (noteId != -1) {
             NotificationManagerCompat.from(context).cancel(noteId)
        }

        // Try to log to UI via MainActivity static helper (we will create this)
        MainActivity.logFromReceiver(message)
    }
}
