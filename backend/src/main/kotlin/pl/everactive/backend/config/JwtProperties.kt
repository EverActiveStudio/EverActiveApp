package pl.everactive.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("everactive.jwt")
data class JwtProperties(
    val secret: String,
    val expiration: Duration,
)
