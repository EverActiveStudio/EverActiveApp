package pl.everactive.clients

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import pl.everactive.shared.ApiResult
import pl.everactive.shared.ApiRoutes
import pl.everactive.shared.PushEventsRequest
import pl.everactive.shared.PushEventsResponse
import pl.everactive.shared.UserDataResponse
import pl.everactive.shared.LoginRequest
import pl.everactive.shared.LoginResponse
import pl.everactive.shared.RegisterRequest
import pl.everactive.shared.serialization.ApiPayloadSerializersModule

class EveractiveApi(
    private val client: HttpClient,
    private val token: EveractiveApiToken,
) {
    private suspend fun HttpRequestBuilder.addAuthorizationHeader() {
        val token = token.get()

        headers {
            append("Authorization", "Bearer $token")
        }
    }

    suspend fun login(request: LoginRequest): LoginResponse {
        val response = client.post(ApiRoutes.Auth.LOGIN) {
            setBody(request)
        }
        // Wypisz w logach co przyszło
        println("DEBUG LOGIN: Status=${response.status} Body=${response.bodyAsText()}")
        return response.body()
    }

    suspend fun register(request: RegisterRequest): ApiResult<LoginResponse> = client
        .post(ApiRoutes.Auth.REGISTER) {
            setBody(request)
        }.body()

    suspend fun pushEvents(request: PushEventsRequest): ApiResult<PushEventsResponse> = client
        .post(ApiRoutes.User.EVENTS) {
            setBody(request)
            addAuthorizationHeader()
        }.body()

    suspend fun managerGetAllUserData(): UserDataResponse = client
        .get(ApiRoutes.Manager.USER_DATA) {
            addAuthorizationHeader()
        }
        .body()

    companion object {
        fun createKtorClient(baseUrl: String) = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                    serializersModule = ApiPayloadSerializersModule
                })
            }

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("KtorClient", message) // Loguje pod tagiem "KtorClient"
                    }
                }
                level = LogLevel.ALL // "ALL" pokaże też treść błędu (JSON), "HEADERS" tylko nagłówki
            }

            defaultRequest {
                url(baseUrl)
                contentType(ContentType.Application.Json)
            }
        }
    }
}
