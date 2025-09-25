package com.just6889.td.tradu_launcher.viewmodel

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.just6889.td.tradu_launcher.data.AuthRepository
import com.just6889.td.tradu_launcher.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _deleteApkAfterInstall = MutableStateFlow(true)
    val deleteApkAfterInstall = _deleteApkAfterInstall.asStateFlow()

    private val _cacheSize = MutableStateFlow(0L)
    val cacheSize = _cacheSize.asStateFlow()

    val username = authRepository.username
    val avatarUrl = authRepository.avatarUrl

    init {
        viewModelScope.launch {
            settingsRepository.deleteApkAfterInstall.collect {
                _deleteApkAfterInstall.value = it
            }
        }
    }

    fun setDeleteApkAfterInstall(shouldDelete: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDeleteApkAfterInstall(shouldDelete)
        }
    }

    fun calculateCacheSize(context: Context) {
        viewModelScope.launch {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val totalSize = downloadsDir?.listFiles { file ->
                file.isFile && file.extension == "apk"
            }?.sumOf { it.length() } ?: 0L
            _cacheSize.value = totalSize
        }
    }

    fun clearCache(context: Context) {
        viewModelScope.launch {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            downloadsDir?.listFiles { file ->
                file.isFile && file.extension == "apk"
            }?.forEach { it.delete() }
            calculateCacheSize(context)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}

class SettingsViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(settingsRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}