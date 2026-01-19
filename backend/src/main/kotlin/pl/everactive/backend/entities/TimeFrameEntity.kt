package pl.everactive.backend.entities

import jakarta.persistence.*

// crude, but should be enough for "now"

@Entity
@Table(name = "time_frames")
class TimeFrameEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "time_frames_id_seq")
    val id: Long? = null,

    @ManyToOne(optional = false)
    val group: GroupEntity,

    @Column(nullable = false)
    val weekDayStart: Int,  // 0-6 incl.
    @Column(nullable = false)
    val hourStart: Int,  // 0-23 incl.

    @Column(nullable = false)
    val weekDayEnd: Int,
    @Column(nullable = false)
    val hourEnd: Int,
)
