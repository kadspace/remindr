package com.remindr.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val noteId = intent.getIntExtra("ITEM_ID", -1)
        if (action == "ACTION_DELETE") {
            val log = "Notification Swiped Away (Dismissed)"
            MainActivity.logFromReceiver(log)
            Toast.makeText(context, log, Toast.LENGTH_SHORT).show()
            return
        }

        val message = "Action received: $action"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

        if (noteId != -1) {
            NotificationManagerCompat.from(context).cancel(noteId)
        }

        MainActivity.logFromReceiver(message)
    }
}
