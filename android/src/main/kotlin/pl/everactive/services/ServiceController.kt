package pl.everactive.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import pl.everactive.clients.EveractiveApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class ServiceController(private val context: Context) : KoinComponent {

    fun startMonitoringService(apiClient: EveractiveApiClient) {
        val intent = Intent(context, SafetyMonitoringService::class.java).apply {
            action = ACTION_START_MONITORING
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopMonitoringService() {
        val intent = Intent(context, SafetyMonitoringService::class.java).apply {
            action = ACTION_STOP_MONITORING
        }
        context.stopService(intent)
    }
}
