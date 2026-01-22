package pl.everactive.clients

import pl.everactive.shared.ApiResult
import pl.everactive.shared.EventDto
import pl.everactive.shared.PushEventsRequest
import pl.everactive.shared.UserDataDto
import pl.everactive.shared.dtos.LoginRequest
import pl.everactive.shared.dtos.LoginResponse
import pl.everactive.shared.dtos.RegisterRequest

// TODO: maybe implement double-hashing?
// TODO: refresh token

class EveractiveApiClient(
    private val api: EveractiveApi,
    private val token: EveractiveApiToken,
) {
    suspend fun login(email: String, rawPassword: String) {
        // FIXME: controller advice -> ApiResult
        val response = api.login(
            LoginRequest(
                email = email,
                password = rawPassword,
            )
        )

        token.set(response.token)
    }

    suspend fun register(name: String, email: String, rawPassword: String): ApiResult.Error<*>? {
        val response = api.register(
            RegisterRequest(
                email = email,
                password = rawPassword,
                name = name,
            )
        )

        return when (response) {
            is ApiResult.Error<*> -> response
            is ApiResult.Success<LoginResponse> -> {
                token.set(response.data.token)
                null
            }
        }
    }

    suspend fun pushEvents(events: List<EventDto>): ApiResult.Error<*>? =
        api.pushEvents(PushEventsRequest(events = events))
            as? ApiResult.Error

    suspend fun managerGetAllUserData(): List<UserDataDto> =
        api.managerGetAllUserData().users
}
