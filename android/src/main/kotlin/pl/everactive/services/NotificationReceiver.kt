package pl.everactive.services

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_DISMISS = "pl.everactive.action.DISMISS"
        const val ACTION_DELETED = "pl.everactive.action.NOTIFICATION_DELETED"
        const val ACTION_RESHOW = "pl.everactive.action.RESHOW_RULES"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DISMISS -> {
                val id = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                if (id != -1) {
                    val manager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.cancel(id)
                }
            }

            ACTION_DELETED -> {
                context.sendBroadcast(Intent(ACTION_RESHOW).setPackage(context.packageName))
            }
        }
    }
}
