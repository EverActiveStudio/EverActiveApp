package pl.everactive.backend.entities

import jakarta.persistence.*


@Entity
@Table(name = "groups")
class GroupEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "groups_id_seq")
    val id: Long? = null,

    @Column(nullable = false, length = 255)
    var name: String,

    @OneToMany(mappedBy = "group")
    val users: MutableList<UserEntity> = mutableListOf(),

    @OneToMany(mappedBy = "group")
    val rules: MutableList<RuleEntity> = mutableListOf(),

    @OneToMany(mappedBy = "group")
    val timeFrames: MutableList<TimeFrameEntity> = mutableListOf(),
)
