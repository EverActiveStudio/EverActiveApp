package pl.everactive.backend.services

import org.springframework.stereotype.Service
import pl.everactive.backend.domain.EventData
import pl.everactive.backend.entities.EventEntity
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class EventProcessor {
    private val states = ConcurrentHashMap<Long, MutableState>()


    suspend fun process(event: EventEntity) {
        val state = states.computeIfAbsent(checkNotNull(event.user.id)) { MutableState() }

        state.lastEventTime = event.timestamp

        when (event.data) {
            is EventData.Location -> {
                state.lastLocation = event.data
            }

            is EventData.Move -> {
                state.lastMoveTime = event.timestamp
            }

            EventData.Ping -> {}
        }
    }

    fun getState(userId: Long): State? = states[userId]

    fun getStates(): Map<Long, State> = states

    interface State {
        val lastLocation: EventData.Location?
        val lastMoveTime: LocalDateTime?
        val lastEventTime: LocalDateTime?
    }

    data class MutableState(
        override var lastLocation: EventData.Location? = null,
        override var lastMoveTime: LocalDateTime? = null,
        override var lastEventTime: LocalDateTime? = null,
    ) : State
}
