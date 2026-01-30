package pl.everactive.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import pl.everactive.MainActivity
import pl.everactive.R
import pl.everactive.clients.EveractiveApiClient
import pl.everactive.shared.Rule
import pl.everactive.utils.PermissionUtils
import java.util.concurrent.atomic.AtomicInteger

class RuleNotificationService(
    private val context: Context,
    private val apiClient: EveractiveApiClient,
) {
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var observeJob: Job? = null
    private var lastTriggeredRules: Set<Rule> = emptySet()
    private val notificationId = AtomicInteger(2000)
    private val activeNotificationIds = mutableSetOf<Int>()

    private val reshowReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == NotificationReceiver.ACTION_RESHOW) {
                if (lastTriggeredRules.isNotEmpty()) {
                    showNotification(lastTriggeredRules.toList())
                }
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "rule_notifications"
        private const val CHANNEL_NAME = "Rule Notifications"
    }

    fun start() {
        if (observeJob != null) return
        createNotificationChannel()

        runCatching {
            val filter = IntentFilter(NotificationReceiver.ACTION_RESHOW)
            ContextCompat.registerReceiver(context, reshowReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }

        observeJob = scope.launch {
            apiClient.triggeredRules.collectLatest { rules ->
                if (rules.isEmpty()) {
                    cancelAllNotifications()
                    lastTriggeredRules = emptySet()
                    return@collectLatest
                }

                val current = rules.toSet()
                val newRules = current.subtract(lastTriggeredRules)
                if (newRules.isNotEmpty()) {
                    showNotification(newRules.toList())
                }
                lastTriggeredRules = current
            }
        }
    }

    fun stop() {
        runCatching {
            context.unregisterReceiver(reshowReceiver)
        }
        observeJob?.cancel()
        observeJob = null
    }

    private fun showNotification(newRules: List<Rule>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !PermissionUtils.hasPostNotificationPermission(context)
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = "Naruszenia zasad"

        val content = if (newRules.size == 1) {
            formatRule(newRules.first())
        } else {
            "Wykryto ${newRules.size} nowe naruszenia zasad"
        }

        val inboxStyle = NotificationCompat.InboxStyle()
        newRules.take(5).forEach { rule ->
            inboxStyle.addLine(formatRule(rule))
        }
        if (newRules.size > 5) {
            inboxStyle.addLine("...i ${newRules.size - 5} wiecej")
        }

        val id = notificationId.incrementAndGet()
        activeNotificationIds.add(id)

        val ignoreIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_DISMISS
            putExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, id)
        }
        val ignorePendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            ignoreIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val deleteIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_DELETED
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(inboxStyle)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setDeleteIntent(deletePendingIntent)
            .addAction(0, "Zignoruj", ignorePendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id, notification)
    }

    private fun formatRule(rule: Rule): String = when (rule) {
        is Rule.NotMoved -> "Brak ruchu przez ${rule.durationMinutes} min"
        is Rule.MissingUpdates -> "Brak aktualizacji przez ${rule.durationMinutes} min"
        is Rule.GeofenceBox -> "Wyjscie poza strefe"
    }

    private fun cancelAllNotifications() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        activeNotificationIds.forEach { id ->
            manager.cancel(id)
        }
        activeNotificationIds.clear()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
