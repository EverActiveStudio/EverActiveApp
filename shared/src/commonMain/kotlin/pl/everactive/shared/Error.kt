package pl.everactive.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ApiResult<T: Any> {
    @Serializable
    @SerialName("success")
    data class Success<T: Any>(val data: T) : ApiResult<T>

    @Serializable
    @SerialName("error")
    data class Error<T: Any>(val type: Type, val message: String = "") : ApiResult<T> {
        enum class Type {
            Generic,
            Validation,
            Conflict
        }
    }
}
