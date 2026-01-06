package pl.everactive.backend.services

import io.konform.validation.Invalid
import kotlinx.coroutines.channels.Channel
import org.springframework.stereotype.Service
import pl.everactive.backend.domain.toDomain
import pl.everactive.backend.entities.EventEntity
import pl.everactive.backend.repositories.EventRepository
import pl.everactive.backend.utils.getLogger
import pl.everactive.backend.utils.toUtcDateTime
import pl.everactive.shared.PushEventsRequest

@Service
class EventService(
    private val requestService: RequestService,
    private val eventRepository: EventRepository,
) {
    private val eventChannel = Channel<EventEntity>(capacity = 1000)

    suspend fun pushEvents(request: PushEventsRequest): PushEventsResult {
        val validationResult = PushEventsRequest.validate(request)
        if (validationResult is Invalid) {
            return PushEventsResult.Failure(validationResult.errors.first().message)
        }

        val user = requestService.userId
        val events = request.events.map {
            EventEntity(
                user = user,
                timestamp = it.timestamp.toUtcDateTime(),
                data = it.toDomain(),
            )
        }

        eventRepository.saveAll(events)
            .collect {
                eventChannel.send(it)
            }

        return PushEventsResult.Success
    }

    sealed interface PushEventsResult {
        data object Success : PushEventsResult
        data class Failure(val reason: String) : PushEventsResult
    }
}
