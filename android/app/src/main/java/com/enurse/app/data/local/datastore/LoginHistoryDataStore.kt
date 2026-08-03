package com.enurse.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Separate file from "auth" on purpose: TokenDataStore.clear() wipes the auth
// file on logout/refresh failure — login history must survive that.
private val Context.loginHistoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "login_history")

@Singleton
class LoginHistoryDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val gson = Gson()
    private val KEY_LOGINS = stringPreferencesKey("saved_logins")
    private val KEY_REMEMBER = booleanPreferencesKey("remember_me")

    val savedLogins: Flow<List<SavedLogin>> =
        context.loginHistoryDataStore.data.map { prefs ->
            val json = prefs[KEY_LOGINS] ?: return@map emptyList()
            runCatching {
                gson.fromJson(json, object : TypeToken<List<SavedLogin>>() {}.type) as List<SavedLogin>
            }.getOrDefault(emptyList())
        }

    val rememberMe: Flow<Boolean> =
        context.loginHistoryDataStore.data.map { it[KEY_REMEMBER] ?: false }

    /** Atomic write of both keys. Read-modify-write logic lives in the repository
     *  (calling DataStore.data inside edit {} would deadlock). */
    suspend fun saveAll(logins: List<SavedLogin>, rememberMe: Boolean) {
        context.loginHistoryDataStore.edit { prefs ->
            prefs[KEY_LOGINS] = gson.toJson(logins)
            prefs[KEY_REMEMBER] = rememberMe
        }
    }
}
