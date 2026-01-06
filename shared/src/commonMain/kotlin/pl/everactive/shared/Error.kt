package pl.everactive.shared

import kotlinx.serialization.Serializable

@Serializable
data class ErrorDto(val message: String)
