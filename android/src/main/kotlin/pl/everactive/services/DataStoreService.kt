package pl.everactive.services

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// TODO: Implement encryption for secure data store

private val Context.secureDataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_datastore")

class DataStoreService(
    private val context: Context
) {

    companion object {
        val SENSITIVITY_KEY = stringPreferencesKey("sensitivity")
    }

    // Nowa metoda do obserwowania zmian czułości
    fun observeSensitivity(): Flow<String> = context.secureDataStore.data.map { preferences ->
        preferences[SENSITIVITY_KEY] ?: "SOFT" // Domyślna wartość
    }
    suspend fun secureSet(key: Preferences.Key<String>, value: String) {
        context.secureDataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    suspend fun secureGet(key: Preferences.Key<String>): String? =
        context.secureDataStore.data.firstOrNull()?.get(key)

    suspend fun remove(key: Preferences.Key<String>) {
        context.secureDataStore.edit { preferences ->
            preferences.remove(key)
        }
    }
}
