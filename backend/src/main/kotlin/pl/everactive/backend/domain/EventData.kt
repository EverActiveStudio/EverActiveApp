package pl.everactive.backend.domain

import kotlinx.serialization.Serializable
import pl.everactive.shared.EventDto

fun EventDto.toDomain(): EventData = when (this) {
    is EventDto.Location -> EventData.Location(latitude, longitude)
    is EventDto.Move -> EventData.Move(steps)
    is EventDto.Ping -> EventData.Ping
}

@Serializable
sealed interface EventData {
    @Serializable
    data object Ping : EventData

    @Serializable
    data class Location(val latitude: Double, val longitude: Double) : EventData

    @Serializable
    data class Move(val steps: Int) : EventData
}
