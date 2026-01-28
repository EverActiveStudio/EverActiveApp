package pl.everactive.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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

    companion object {
        private const val CHANNEL_ID = "rule_notifications"
        private const val CHANNEL_NAME = "Rule Notifications"
    }

    fun start() {
        if (observeJob != null) return
        createNotificationChannel()
        observeJob = scope.launch {
            apiClient.triggeredRules.collectLatest { rules ->
                if (rules.isEmpty()) {
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

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(inboxStyle)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId.incrementAndGet(), notification)
    }

    private fun formatRule(rule: Rule): String = when (rule) {
        is Rule.NotMoved -> "Brak ruchu przez ${rule.durationMinutes} min"
        is Rule.MissingUpdates -> "Brak aktualizacji przez ${rule.durationMinutes} min"
        is Rule.GeofenceBox -> "Wyjscie poza strefe"
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
