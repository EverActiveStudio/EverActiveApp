// Plik: eve/kotlin/pl/everactive/services/AlertManager.kt
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

    private val _alertStatus = MutableStateFlow(AlertStatus.NONE)
    val alertStatus: StateFlow<AlertStatus> = _alertStatus.asStateFlow()

    // ZMIANA: Startowa wartość 10
    private val _timeRemaining = MutableStateFlow(10)
    val timeRemaining: StateFlow<Int> = _timeRemaining.asStateFlow()

    private var countdownJob: Job? = null

    fun triggerSOS() {
        if (_alertStatus.value != AlertStatus.NONE) return

        _alertStatus.value = AlertStatus.PENDING
        // ZMIANA: Reset licznika do 10 przy uruchomieniu
        _timeRemaining.value = 10

        startCountdown()
    }

    fun cancelSOS() {
        countdownJob?.cancel()
        _alertStatus.value = AlertStatus.NONE
        // ZMIANA: Reset do 10 przy anulowaniu
        _timeRemaining.value = 10
    }

    fun closeAlert() {
        _alertStatus.value = AlertStatus.NONE
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            try {
                while (_timeRemaining.value > 0) {
                    delay(1000)
                    _timeRemaining.value -= 1
                }
                sendAlertToApi()
            } catch (e: Exception) {
                e.printStackTrace()
                // W razie błędu odliczania, wymuszamy przejście dalej
                _alertStatus.value = AlertStatus.SENT
            }
        }
    }

    private suspend fun sendAlertToApi() {
        try {
            apiClient.pushEvents(
                listOf(EventDto.Ping(timestamp = System.currentTimeMillis()))
            )
            _alertStatus.value = AlertStatus.SENT
        } catch (e: Exception) {
            e.printStackTrace()
            // Nawet przy błędzie sieci pokazujemy czerwony ekran
            _alertStatus.value = AlertStatus.SENT
        }
    }
}
