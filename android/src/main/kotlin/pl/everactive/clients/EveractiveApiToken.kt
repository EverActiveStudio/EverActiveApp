package pl.everactive.clients

import androidx.datastore.preferences.core.stringPreferencesKey
import pl.everactive.services.DataStoreService

class EveractiveApiToken(
    private val dataStoreService: DataStoreService,
) {
    suspend fun get(): String? = dataStoreService.secureGet(TOKEN_KEY)
    suspend fun set(token: String) {
        dataStoreService.secureSet(TOKEN_KEY, token)
    }

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("api_token")
    }
}
