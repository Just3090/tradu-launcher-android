package com.just6889.td.tradu_launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

class AuthRepository(private val context: Context, private val apiService: ApiService) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val AVATAR_URL_KEY = stringPreferencesKey("avatar_url")
    }

    val authToken: Flow<String?> = context.authDataStore.data.map { it[TOKEN_KEY] }
    val username: Flow<String?> = context.authDataStore.data.map { it[USERNAME_KEY] }
    val avatarUrl: Flow<String?> = context.authDataStore.data.map { it[AVATAR_URL_KEY] }

    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(LoginRequest(username, password))
            if (response.isSuccessful && response.body() != null) {
                val loginData = response.body()!!
                saveAuthData(loginData.access, loginData.username, loginData.avatar_url)
                Result.success(loginData)
            } else {
                Result.failure(Exception("Usuario o contraseña incorrectos"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveAuthData(token: String, username: String?, avatarUrl: String?) {
        context.authDataStore.edit { settings ->
            settings[TOKEN_KEY] = token
            username?.let { settings[USERNAME_KEY] = it }
            avatarUrl?.let { settings[AVATAR_URL_KEY] = it }
        }
    }

    suspend fun logout() {
        context.authDataStore.edit {
            it.clear()
        }
    }
}