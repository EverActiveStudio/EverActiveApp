package pl.everactive.backend.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_id_seq")
    val id: Long? = null,

    @Column(nullable = false, length = 255, unique = true)
    val email: String,

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(nullable = false, length = 255)
    var password: String,
)
