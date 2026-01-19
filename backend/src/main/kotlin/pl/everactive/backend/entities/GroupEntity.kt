package pl.everactive.backend.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table


@Entity
@Table(name = "groups")
class GroupEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "groups_id_seq")
    val id: Long,

    @Column(nullable = false, length = 255)
    var name: String,

    @OneToMany(mappedBy = "group_id")
    val users: MutableList<UserEntity> = mutableListOf(),
)
