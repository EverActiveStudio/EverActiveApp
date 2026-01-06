package pl.everactive.backend.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory


inline fun <reified T: Any> T.getLogger(): Logger = when (T::class.isCompanion) {
    true -> LoggerFactory.getLogger(T::class.java.enclosingClass)
    false -> LoggerFactory.getLogger(T::class.java)
}
