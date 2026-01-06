package pl.everactive.shared

import kotlinx.serialization.Serializable

@Serializable
sealed interface EventDto {
    val timestamp: Long
}

@Serializable
data class PingEventDto(override val timestamp: Long) : EventDto

@Serializable
data class LocationEventDto(
    override val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
) : EventDto

@Serializable
data class MoveEventDto(
    override val timestamp: Long,
    val steps: Int,
) : EventDto

@Serializable
data class PushEventsRequest(val events: List<EventDto>)
