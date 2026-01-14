package pl.everactive.backend.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset


inline fun <reified T : Any> T.getLogger(): Logger = when (T::class.isCompanion) {
    true -> LoggerFactory.getLogger(T::class.java.enclosingClass)
    false -> LoggerFactory.getLogger(T::class.java)
}

fun Long.toUtcDateTime(): LocalDateTime = LocalDateTime.ofInstant(
    Instant.ofEpochMilli(this),
    ZoneOffset.UTC,
)
