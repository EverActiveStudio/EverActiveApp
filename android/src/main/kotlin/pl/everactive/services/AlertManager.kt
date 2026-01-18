package pl.everactive.services

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.everactive.AlertStatus
import pl.everactive.clients.EveractiveApiClient
import pl.everactive.shared.EventDto

class AlertManager(
    private val apiClient: EveractiveApiClient
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // We observe the AlertStatus defined in DashboardScreen (pl.everactive package)
    private val _alertStatus = MutableStateFlow(AlertStatus.NONE)
    val alertStatus: StateFlow<AlertStatus> = _alertStatus.asStateFlow()

    private val _timeRemaining = MutableStateFlow(5)
    val timeRemaining: StateFlow<Int> = _timeRemaining.asStateFlow()

    private var countdownJob: Job? = null

    fun triggerSOS() {
        if (_alertStatus.value != AlertStatus.NONE) return

        _alertStatus.value = AlertStatus.PENDING
        _timeRemaining.value = 5

        startCountdown()
    }

    fun cancelSOS() {
        countdownJob?.cancel()
        _alertStatus.value = AlertStatus.NONE
        _timeRemaining.value = 5
    }

    fun closeAlert() {
        _alertStatus.value = AlertStatus.NONE
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            while (_timeRemaining.value > 0) {
                delay(1000)
                _timeRemaining.value -= 1
            }
            sendAlertToApi()
        }
    }

    private suspend fun sendAlertToApi() {
        val result = apiClient.pushEvents(
            listOf(EventDto.Ping(timestamp = System.currentTimeMillis()))
        )
        // Update state to SENT to notify UI
        _alertStatus.value = AlertStatus.SENT
    }
}
