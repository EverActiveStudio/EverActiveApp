package pl.everactive.backend.services

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import pl.everactive.backend.config.JwtProperties
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

        val claims = JwtClaimsSet.builder()
            .issuer("self")
            .issuedAt(now)
            .expiresAt(now + props.expiration)
            .subject(authentication.name)
            .claim("scope", scope)
            .build()

        val encoderParameters = JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(),
            claims,
        )

        return jwtEncoder.encode(encoderParameters).tokenValue
    }
}
