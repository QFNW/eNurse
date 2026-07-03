package com.enurse.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

@Singleton
class TokenDataStore @Inject constructor(private val context: Context) {

    private val KEY_ACCESS = stringPreferencesKey("access_token")
    private val KEY_REFRESH = stringPreferencesKey("refresh_token")
    private val KEY_USER_ROLE = stringPreferencesKey("user_role")
    private val KEY_USER_ID = stringPreferencesKey("user_id")

    val accessToken: Flow<String?> = context.dataStore.data.map { it[KEY_ACCESS] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[KEY_REFRESH] }
    val userRole: Flow<String?> = context.dataStore.data.map { it[KEY_USER_ROLE] }
    val userId: Flow<String?> = context.dataStore.data.map { it[KEY_USER_ID] }

    suspend fun saveTokens(access: String, refresh: String) {
        context.dataStore.edit {
            it[KEY_ACCESS] = access
            it[KEY_REFRESH] = refresh
        }
    }

    suspend fun saveUserInfo(userId: String, role: String) {
        context.dataStore.edit {
            it[KEY_USER_ID] = userId
            it[KEY_USER_ROLE] = role
        }
    }

    suspend fun getPreference(key: String): String? {
        return context.dataStore.data.map { it[stringPreferencesKey(key)] }.first()
    }

    suspend fun setPreference(key: String, value: String) {
        context.dataStore.edit { it[stringPreferencesKey(key)] = value }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
