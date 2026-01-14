package pl.everactive.services

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull

// TODO: Implement encryption for secure data store

private val Context.secureDataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_datastore")

class DataStoreService(
    private val context: Context
) {
    suspend fun secureSet(key: Preferences.Key<String>, value: String) {
        context.secureDataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    suspend fun secureGet(key: Preferences.Key<String>): String? =
        context.secureDataStore.data.firstOrNull()?.get(key)
}
