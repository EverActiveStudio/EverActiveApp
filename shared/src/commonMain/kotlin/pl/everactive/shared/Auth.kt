package pl.everactive.shared

import io.konform.validation.Validation
import io.konform.validation.jsonschema.maxLength
import io.konform.validation.jsonschema.minLength
import io.konform.validation.jsonschema.pattern
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(val token: String) : ApiPayload

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
) {
    companion object {
        val validate = Validation {
            RegisterRequest::email {
                pattern("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$") hint "Invalid email format"
            }
            RegisterRequest::password {
                minLength(8) hint "Password must be at least 8 characters long"
                maxLength(72) hint "Password is too long"
            }
        }
    }
}
