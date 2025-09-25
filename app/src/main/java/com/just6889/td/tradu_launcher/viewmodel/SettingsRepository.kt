package com.just6889.td.tradu_launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val DELETE_APK_AFTER_INSTALL = booleanPreferencesKey("delete_apk_after_install")
    }

    val deleteApkAfterInstall: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[DELETE_APK_AFTER_INSTALL] ?: true
        }

    suspend fun setDeleteApkAfterInstall(shouldDelete: Boolean) {
        context.settingsDataStore.edit { settings ->
            settings[DELETE_APK_AFTER_INSTALL] = shouldDelete
        }
    }
}