package pl.everactive.backend.entities

import jakarta.persistence.*
import kotlinx.serialization.json.Json
import pl.everactive.shared.Rule


@Entity
@Table(name = "rules")
class RuleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rules_id_seq")
    val id: Long? = null,

    @ManyToOne(optional = false)
    val group: GroupEntity,

    @Convert(converter = RuleConverter::class)
    @Column(columnDefinition = "json", nullable = false)
    val rule: Rule,
) {
    @Converter
    class RuleConverter : AttributeConverter<Rule, String> {
        override fun convertToDatabaseColumn(attribute: Rule): String = Json.encodeToString(attribute)
        override fun convertToEntityAttribute(dbData: String): Rule = Json.decodeFromString(dbData)
    }
}
