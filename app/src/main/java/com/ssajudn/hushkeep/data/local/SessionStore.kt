package com.ssajudn.hushkeep.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ssajudn.hushkeep.domain.repository.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "hushkeep_session")

data class StoredSession(
    val userId: String,
    val email: String?,
    val username: String?,
)

class SessionStore(private val context: Context) {
    private val userIdKey = stringPreferencesKey("user_id")
    private val emailKey = stringPreferencesKey("email")
    private val usernameKey = stringPreferencesKey("username")

    val session: Flow<StoredSession?> = context.sessionDataStore.data.map { preferences ->
        preferences[userIdKey]?.let { userId ->
            StoredSession(
                userId = userId,
                email = preferences[emailKey],
                username = preferences[usernameKey],
            )
        }
    }

    suspend fun save(user: AuthUser) {
        context.sessionDataStore.edit { preferences ->
            preferences[userIdKey] = user.id
            user.email?.let { preferences[emailKey] = it } ?: preferences.remove(emailKey)
            user.username?.let { preferences[usernameKey] = it } ?: preferences.remove(usernameKey)
        }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { preferences ->
            preferences.remove(userIdKey)
            preferences.remove(emailKey)
            preferences.remove(usernameKey)
        }
    }
}
