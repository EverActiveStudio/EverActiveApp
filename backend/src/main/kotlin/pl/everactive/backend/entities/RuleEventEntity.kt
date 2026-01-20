package pl.everactive.backend.entities

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "rule_events")
class RuleEventEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rule_events_id_seq")
    val id: Long? = null,

    @ManyToOne(optional = false)
    val rule: RuleEntity,

    @ManyToOne(optional = false)
    val user: UserEntity,

    @Column(nullable = false)
    val timestamp: LocalDateTime,

    @Column(nullable = false)
    val isFailed: Boolean,
)
