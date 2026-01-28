package pl.everactive.shared

import io.konform.validation.Validation
import io.konform.validation.constraints.minItems
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface EventDto {
    val timestamp: Long

    @Serializable
    @SerialName("Ping")
    data class Ping(override val timestamp: Long) : EventDto

    @Serializable
    @SerialName("SignificantMotion")
    data class SignificantMotion(override val timestamp: Long, val totalDelta: Double) : EventDto

    @Serializable
    @SerialName("SOS")
    data class SOS(override val timestamp: Long, val cancel: Boolean, val latitude: Double, val longitude: Double) : EventDto

    @Serializable
    @SerialName("Fall")
    data class Fall(override val timestamp: Long, val latitude: Double, val longitude: Double) : EventDto

    @Serializable
    @SerialName("Location")
    data class Location(
        override val timestamp: Long,
        val latitude: Double,
        val longitude: Double,
    ) : EventDto

    @Serializable
    @SerialName("Move")
    data class Move(
        override val timestamp: Long,
        val steps: Int,
    ) : EventDto
}

@Serializable
data class PushEventsRequest(val events: List<EventDto>) {
    companion object {
        val validate = Validation {
            PushEventsRequest::events {
                minItems(1) hint "At least one event is required"
                constrain("Events must be ordered by timestamp") { events ->
                    events.asSequence()
                        .zipWithNext()
                        .all { (curr, next) -> curr.timestamp < next.timestamp }
                }
            }
        }
    }
}

@Serializable
data class PushEventsResponse(val triggeredRules: List<Rule>)
