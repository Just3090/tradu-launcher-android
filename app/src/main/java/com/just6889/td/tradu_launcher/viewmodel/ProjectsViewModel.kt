package com.just6889.td.tradu_launcher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.just6889.td.tradu_launcher.data.Project
import com.just6889.td.tradu_launcher.data.ProjectRepository
import com.just6889.td.tradu_launcher.data.ProjectInstallState
import com.just6889.td.tradu_launcher.data.ApkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProjectsViewModel(private val repository: ProjectRepository) : ViewModel() {

    // Progreso de descarga
    private val _downloadProgress = kotlinx.coroutines.flow.MutableStateFlow(0)
    val downloadProgress: kotlinx.coroutines.flow.StateFlow<Int> = _downloadProgress

    // Proyecto en descarga
    private val _downloadingProject = kotlinx.coroutines.flow.MutableStateFlow<Project?>(null)
    val downloadingProject: kotlinx.coroutines.flow.StateFlow<Project?> = _downloadingProject

    // Nuevo: callback para avisar a la UI que la descarga terminó
    private val _showInstallPrompt = MutableStateFlow<Project?>(null)
    val showInstallPrompt: StateFlow<Project?> = _showInstallPrompt

    fun startDownload(context: android.content.Context, project: Project) {
        _downloadingProject.value = project
        _downloadProgress.value = 0
        com.just6889.td.tradu_launcher.data.ApkUtils.downloadApk(
            context,
            project,
            onDownloadComplete = {
                _downloadProgress.value = 100
                _downloadingProject.value = null
                // Solo mostrar notificación y alerta en UI, NO instalar automáticamente
                _showInstallPrompt.value = project
            },
            onProgress = { progress ->
                _downloadProgress.value = progress
            }
        )
    }

    fun clearInstallPrompt() {
        _showInstallPrompt.value = null
    }

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Estado de instalación por proyecto
    private val _projectInstallStates = MutableStateFlow<Map<String, ProjectInstallState>>(emptyMap())
    val projectInstallStates: StateFlow<Map<String, ProjectInstallState>> = _projectInstallStates

    init {
        android.util.Log.d("ProjectsViewModel", "init: llamando a loadProjects()")
        loadProjects()
    }

    fun loadProjects() {
        android.util.Log.d("ProjectsViewModel", "loadProjects() ejecutado")
        _isLoading.value = true
        viewModelScope.launch {
            val data = repository.loadProjectsData()
            android.util.Log.d("ProjectsViewModel", "Resultado de loadProjectsData: ${'$'}data")
            val proyectos = data?.proyectos ?: emptyList()
            _projects.value = proyectos
            // Calcular estado de instalación por proyecto
            val context = getApplicationContextSafely()
            val stateMap = mutableMapOf<String, ProjectInstallState>()
            if (context != null) {
                for (project in proyectos) {
                    val apkFile = ApkUtils.getApkFile(context, project)
                    // Detectar packageName real si el APK existe
                    var realPackageName = project.packageName
                    if (apkFile.exists()) {
                        ApkUtils.getPackageNameFromApk(context, apkFile)?.let {
                            realPackageName = it
                        }
                    }
                    val isInstalled = ApkUtils.isApkInstalled(context, realPackageName)
                    val state = when {
                        isInstalled -> ProjectInstallState.INSTALLED
                        apkFile.exists() -> ProjectInstallState.DOWNLOADED
                        else -> ProjectInstallState.NOT_DOWNLOADED
                    }
                    stateMap[project.id_proyecto] = state
                }
            }
            _projectInstallStates.value = stateMap
            _isLoading.value = false
            android.util.Log.d("ProjectsViewModel", "Proyectos cargados: ${'$'}{_projects.value.size}")
        }
    }

    // Helper para obtener contexto de aplicación de forma segura
    private fun getApplicationContextSafely(): android.content.Context? {
        return try {
            val clazz = Class.forName("android.app.ActivityThread")
            val method = clazz.getMethod("currentApplication")
            method.invoke(null) as? android.content.Context
        } catch (e: Exception) {
            null
        }
    }

    // Permite forzar actualización del estado de un proyecto (tras instalar, etc)
    fun refreshProjectState(context: android.content.Context, project: Project) {
        val apkFile = ApkUtils.getApkFile(context, project)
        var realPackageName = project.packageName
        if (apkFile.exists()) {
            ApkUtils.getPackageNameFromApk(context, apkFile)?.let {
                realPackageName = it
            }
        }
        val isInstalled = ApkUtils.isApkInstalled(context, realPackageName)
        val state = when {
            isInstalled -> ProjectInstallState.INSTALLED
            apkFile.exists() -> ProjectInstallState.DOWNLOADED
            else -> ProjectInstallState.NOT_DOWNLOADED
        }
        val newMap = _projectInstallStates.value.toMutableMap()
        newMap[project.id_proyecto] = state
        _projectInstallStates.value = newMap
    }
}
