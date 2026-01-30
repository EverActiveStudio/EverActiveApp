package pl.everactive.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ApiPayload

@Serializable
sealed interface ApiResult<T: ApiPayload> {
    @Serializable
    @SerialName("success")
    data class Success<T: ApiPayload>(val data: T) : ApiResult<T>

    @Serializable
    @SerialName("error")
    data class Error<T: ApiPayload>(val errorType: Type, val message: String = "") : ApiResult<T> {
        enum class Type {
            Generic,
            Validation,
            Conflict
        }
    }
}
