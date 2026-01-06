package pl.everactive.backend.entities

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Converter
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import kotlinx.serialization.json.Json
import pl.everactive.backend.domain.EventData
import java.time.LocalDateTime

@Entity
@Table(name = "events")
class EventEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "events_id_seq")
    val id: Long? = null,

    @ManyToOne(optional = false)
    val user: UserEntity,

    @Column(nullable = false)
    val timestamp: LocalDateTime,

    @Convert(converter = EventDataConverter::class)
    @Column(columnDefinition = "json", nullable = false)
    val data: EventData,
) {
    @Converter
    class EventDataConverter : AttributeConverter<EventData, String> {
        override fun convertToDatabaseColumn(attribute: EventData): String {
            return Json.encodeToString(attribute)
        }

        override fun convertToEntityAttribute(dbData: String): EventData {
            return Json.decodeFromString(dbData)
        }
    }
}
