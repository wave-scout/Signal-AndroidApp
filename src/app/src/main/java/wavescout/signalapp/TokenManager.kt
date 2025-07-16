package wavescout.signalapp.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Extension property för DataStore (måste ligga på top-level)
val Context.dataStore by preferencesDataStore(name = "settings")

object TokenManager {
    private val tokenKey = stringPreferencesKey("signalRToken")

    // Spara token
    suspend fun saveToken(context: Context, token: String) {
        context.dataStore.edit { settings ->
            settings[tokenKey] = token
        }
    }

    // Hämta token (Flow för Compose eller ViewModel)
    fun getTokenFlow(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[tokenKey] ?: ""
        }
    }

    // Hämta token en gång (ex. i coroutine eller ViewModel)
    suspend fun getToken(context: Context): String? {
        return context.dataStore.data
            .map { it[tokenKey] }
            .first()
    }
}