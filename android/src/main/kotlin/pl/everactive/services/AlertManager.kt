package pl.everactive.services

import android.location.Location
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

    private val _timeRemaining = MutableStateFlow(10)
    val timeRemaining: StateFlow<Int> = _timeRemaining.asStateFlow()

    private var countdownJob: Job? = null

    // Przechowujemy typ zdarzenia i lokalizację do wysłania po odliczaniu
    private var pendingEventType: EventType? = null
    private var lastKnownLocation: Location? = null

    enum class EventType { FALL, SOS }

    // Metoda do aktualizacji lokalizacji z serwisu w tle
    fun updateLocation(location: Location) {
        lastKnownLocation = location
    }

    // Wywołanie ręczne (przycisk SOS)
    fun triggerSOS() {
        startAlertProcess(EventType.SOS)
    }

    // Wywołanie automatyczne (wykryty upadek)
    fun triggerFall() {
        startAlertProcess(EventType.FALL)
    }

    private fun startAlertProcess(type: EventType) {
        if (_alertStatus.value != AlertStatus.NONE) return

        pendingEventType = type
        _alertStatus.value = AlertStatus.PENDING
        _timeRemaining.value = 10
        startCountdown()
    }

    fun cancelSOS() {
        countdownJob?.cancel()
        _alertStatus.value = AlertStatus.NONE
        _timeRemaining.value = 10
        pendingEventType = null
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
                // W razie błędu odliczania, wymuszamy przejście do wysyłki (bezpieczeństwo)
                sendAlertToApi()
            }
        }
    }

    private suspend fun sendAlertToApi() {
        val lat = lastKnownLocation?.latitude ?: 0.0
        val lon = lastKnownLocation?.longitude ?: 0.0
        val timestamp = System.currentTimeMillis()

        // Tworzymy odpowiedni obiekt DTO w zależności od typu alarmu
        val eventToSend = when (pendingEventType) {
            EventType.FALL -> EventDto.Fall(timestamp, lat, lon)
            EventType.SOS -> EventDto.SOS(timestamp, cancel = false, lat, lon)
            else -> EventDto.Ping(timestamp) // Fallback
        }

        try {
            apiClient.pushEvents(listOf(eventToSend))
            _alertStatus.value = AlertStatus.SENT
        } catch (e: Exception) {
            e.printStackTrace()
            // Nawet przy błędzie sieci zmieniamy status na SENT, aby poinformować użytkownika o zakończeniu procedury
            _alertStatus.value = AlertStatus.SENT
        }
    }
}
