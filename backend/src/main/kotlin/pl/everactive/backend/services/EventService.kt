package pl.everactive.backend.services

import io.konform.validation.Invalid
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
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
    private val userStateService: UserStateService,
) {
    private val logger = getLogger()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val channel = Channel<EventEntity>(capacity = 1000)

    suspend fun pushEvents(request: PushEventsRequest): PushEventsResult = coroutineScope {
        val validationResult = PushEventsRequest.validate(request)
        if (validationResult is Invalid) {
            return@coroutineScope PushEventsResult.Failure(validationResult.errors.first().message)
        }

        val user = requestService.userId
        val events = request.events.map {
            EventEntity(
                user = user,
                timestamp = it.timestamp.toUtcDateTime(),
                data = it.toDomain(),
            )
        }

        withContext(Dispatchers.IO) {
            eventRepository.saveAll(events)
                .forEach {
                    channel.send(it)
                }
        }

        PushEventsResult.Success
    }

    @PostConstruct
    fun initialize() {
        scope.launch {
            channel.consumeAsFlow().collect { event ->
                try {
                    userStateService.update(event)
                } catch (e: Exception) {
                    logger.error("Error processing event ${event.id}", e)
                }
            }
        }
    }

    @PreDestroy
    fun destroy() {
        channel.close()
        scope.cancel()
    }

    sealed interface PushEventsResult {
        data object Success : PushEventsResult
        data class Failure(val reason: String) : PushEventsResult
    }
}
