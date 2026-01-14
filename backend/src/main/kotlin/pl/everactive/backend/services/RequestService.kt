package pl.everactive.backend.services

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.web.context.annotation.RequestScope
import pl.everactive.backend.entities.UserEntity
import pl.everactive.backend.repositories.UserRepository
import kotlin.jvm.optionals.getOrNull

@Service
@RequestScope
class RequestService(
    private val userRepository: UserRepository,
) {
    val user: UserEntity by lazy {
        when (val principal = SecurityContextHolder.getContext().authentication?.principal) {
            is UserEntity -> principal
            is Jwt -> {
                val userId = checkNotNull(principal.claims[TokenService.USER_ID_CLAIM] as? Long) {
                    "JWT is missing ${TokenService.USER_ID_CLAIM} claim"
                }

                checkNotNull(userRepository.findById(userId).getOrNull()) {
                    "User with id $userId not found"
                }
            }

            null -> error("No authenticated principal found")
            else -> error("Unknown principal type: ${principal::class.java.name}")
        }
    }

    val userId: UserEntity by lazy {
        when (val principal = SecurityContextHolder.getContext().authentication?.principal) {
            is UserEntity -> principal
            is Jwt -> {
                val userId = checkNotNull(principal.claims[TokenService.USER_ID_CLAIM] as? Long) {
                    "JWT is missing ${TokenService.USER_ID_CLAIM} claim"
                }

                userRepository.getReferenceById(userId)
            }

            null -> error("No authenticated principal found")
            else -> error("Unknown principal type: ${principal::class.java.name}")
        }
    }
}
