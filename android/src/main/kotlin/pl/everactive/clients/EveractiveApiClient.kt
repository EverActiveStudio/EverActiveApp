package pl.everactive.clients

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.everactive.shared.ApiResult
import pl.everactive.shared.EventDto
import pl.everactive.shared.PushEventsRequest
import pl.everactive.shared.PushEventsResponse
import pl.everactive.shared.Rule
import pl.everactive.shared.UserDataDto
import pl.everactive.shared.dtos.LoginRequest
import pl.everactive.shared.dtos.LoginResponse
import pl.everactive.shared.dtos.RegisterRequest
import kotlin.collections.emptyList

// TODO: maybe implement double-hashing?
// TODO: refresh token

class EveractiveApiClient(
    private val api: EveractiveApi,
    private val token: EveractiveApiToken,
) {
    private val mutableTriggeredRules: MutableStateFlow<List<Rule>> = MutableStateFlow(emptyList())

    val triggeredRules: StateFlow<List<Rule>> = mutableTriggeredRules.asStateFlow()

    suspend fun login(email: String, rawPassword: String) {
        token.clear()

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
        token.clear()
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

    suspend fun pushEvents(events: List<EventDto>): ApiResult.Error<*>? {
        when (val response = api.pushEvents(PushEventsRequest(events = events))) {
            is ApiResult.Error<*> -> {
                return response
            }

            is ApiResult.Success<PushEventsResponse> -> {
                mutableTriggeredRules.value = response.data.triggeredRules
                return null
            }
        }
    }

    suspend fun managerGetAllUserData(): List<UserDataDto> =
        api.managerGetAllUserData().users
}
