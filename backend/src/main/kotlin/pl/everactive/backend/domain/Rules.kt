package pl.everactive.backend.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
sealed interface Rule {
    @Serializable
    @SerialName("NotMoved")
    data class NotMoved(val durationMinutes: Int) : Rule

    @Serializable
    @SerialName("MissingUpdates")
    data class MissingUpdates(val durationMinutes: Int) : Rule

    @Serializable
    @SerialName("GeofenceBox")
    data class GeofenceBox(
        val minLatitude: Double,
        val maxLatitude: Double,
        val minLongitude: Double,
        val maxLongitude: Double,
    ) : Rule
}
