package pl.everactive.clients

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import pl.everactive.shared.ApiResult
import pl.everactive.shared.ApiRoutes
import pl.everactive.shared.PushEventsRequest
import pl.everactive.shared.UserDataResponse
import pl.everactive.shared.dtos.LoginRequest
import pl.everactive.shared.dtos.LoginResponse
import pl.everactive.shared.dtos.RegisterRequest

class EveractiveApi(private val client: HttpClient) {
    suspend fun login(request: LoginRequest): LoginResponse = client
        .post(ApiRoutes.Auth.LOGIN) {
            setBody(request)
        }.body()

    suspend fun register(request: RegisterRequest): ApiResult<LoginResponse> = client
        .post(ApiRoutes.Auth.REGISTER) {
            setBody(request)
        }.body()

    suspend fun pushEvents(request: PushEventsRequest): ApiResult<Unit> = client
        .post(ApiRoutes.User.EVENTS) {
            setBody(request)
        }.body()

    suspend fun managerGetAllUserData(): UserDataResponse = client
        .get(ApiRoutes.Manager.USER_DATA)
        .body()

    companion object {
        fun createKtorClient(
            baseUrl: String,
            apiToken: EveractiveApiToken,
        ) = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                })
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        apiToken.get()
                            ?.let { BearerTokens(it, "") }
                    }

                    refreshTokens {
                        // FIXME: implement token refresh
                        null
                    }
                }
            }

            defaultRequest {
                url(baseUrl)
                contentType(ContentType.Application.Json)
            }
        }
    }
}
