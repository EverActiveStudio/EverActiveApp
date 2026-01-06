package pl.everactive.backend.services

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import pl.everactive.backend.config.JwtProperties
import pl.everactive.backend.entities.UserEntity
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class TokenService(
    private val jwtEncoder: JwtEncoder,
    private val props: JwtProperties,
) {
    fun generate(authentication: Authentication): String {
        val now = Instant.now()
        val scope = authentication.authorities
            .mapNotNull { it.authority }

        val user = checkNotNull(authentication.principal as? UserEntity) {
            "Principal is not of type UserEntity"
        }

        val claims = JwtClaimsSet.builder()
            .issuer("self")
            .issuedAt(now)
            .expiresAt(now + props.expiration)
            .subject(authentication.name)
            .claim("scope", scope)
            .claim(USER_ID_CLAIM, user.id)
            .build()

        val encoderParameters = JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(),
            claims,
        )

        return jwtEncoder.encode(encoderParameters).tokenValue
    }

    companion object {
        const val USER_ID_CLAIM = "user.id"
    }
}
