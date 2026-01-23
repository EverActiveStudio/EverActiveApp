package pl.everactive.backend.entities

import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import pl.everactive.backend.config.Role

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
    @get:JvmName("getPasswordInternal")
    var password: String,

    @Column(nullable = false, length = 31)
    @Enumerated(EnumType.STRING)
    var role: Role,

    @ManyToOne(optional = true, fetch = FetchType.EAGER)
    var group: GroupEntity? = null,
) : UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority> = listOf(SimpleGrantedAuthority("ROLE_${role.name}"))
    override fun getPassword(): String = password
    override fun getUsername(): String = email
}
