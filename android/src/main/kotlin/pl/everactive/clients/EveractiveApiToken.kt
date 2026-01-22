package pl.everactive.clients

import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.everactive.services.DataStoreService
import java.util.*

class EveractiveApiToken(
    private val dataStoreService: DataStoreService,
) {
    suspend fun get(): String? = dataStoreService.secureGet(TOKEN_KEY)
    suspend fun set(token: String) {
        dataStoreService.secureSet(TOKEN_KEY, token)
    }

    suspend fun clear() {
        dataStoreService.remove(TOKEN_KEY)
    }

    // TODO: refactor
    suspend fun getRole(): Role? {
        val token = get() ?: return null

        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val decoded = Base64.getUrlDecoder().decode(parts[1])
            val jsonString = String(decoded, Charsets.UTF_8)
            val data = json.decodeFromString<TokenData>(jsonString)

            Role.fromScope(data.scope)
        } catch (e: Exception) {
            null
        }
    }

    @Serializable
    data class TokenData(val scope: List<String>)

    enum class Role {
        User, Manager;

        companion object {
            fun fromScope(scope: List<String>): Role? = scope.find {
                it.startsWith("ROLE_")
            }?.let {
                when (it.removePrefix("ROLE_")) {
                    "User" -> User
                    "Manager" -> Manager
                    else -> null
                }
            }
        }
    }

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("api_token")
        private val json = Json { ignoreUnknownKeys = true }
    }
}
