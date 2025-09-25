package com.just6889.td.tradu_launcher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.just6889.td.tradu_launcher.data.Project
import com.just6889.td.tradu_launcher.data.ProjectRepository
import com.just6889.td.tradu_launcher.data.ProjectInstallState
import com.just6889.td.tradu_launcher.data.ApkUtils
import com.just6889.td.tradu_launcher.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProjectsViewModel(private val repository: ProjectRepository) : ViewModel() {
    /**
     * Detección silenciosa y robusta de apps instaladas al cargar proyectos o volver al foreground.
     * Si una app está instalada y no hay versión local, la marca como instalada (versión del JSON).
     * Si una app NO está instalada pero hay versión local, la borra (refuerzo extra).
     */
    fun detectInstalledAppsSilently(context: android.content.Context, projects: List<Project>) {
        val prefs = context.getSharedPreferences("launcher_versions", android.content.Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (project in projects) {
            val apkFile = ApkUtils.getApkFile(context, project)
            var realPackageName = project.packageName
            if (apkFile.exists()) {
                ApkUtils.getPackageNameFromApk(context, apkFile)?.let {
                    realPackageName = it
                }
            }
            val isInstalled = ApkUtils.isApkInstalled(context, realPackageName)

            val localVersion = prefs.getString("installed_version_${project.id_proyecto}", null)

            if (isInstalled && localVersion == null) {
                editor.putString("installed_version_${project.id_proyecto}", project.version)
                android.util.Log.d("ProjectsViewModel", "[detectInstalledAppsSilently] App instalada externamente detectada: $realPackageName (id: ${project.id_proyecto}) -> se marca como INSTALLED y version=${project.version}")
            } else if (!isInstalled && localVersion != null) {
                editor.remove("installed_version_${project.id_proyecto}")
                android.util.Log.d("ProjectsViewModel", "[detectInstalledAppsSilently] App desinstalada detectada: $realPackageName (id: ${project.id_proyecto}) -> se borra la versión local.")
            }
        }
        editor.apply()
        for (project in projects) {
            refreshProjectState(context, project)
        }
    }

    // Progreso de descarga
    private val _downloadProgress = kotlinx.coroutines.flow.MutableStateFlow(0)
    val downloadProgress: kotlinx.coroutines.flow.StateFlow<Int> = _downloadProgress

    // Proyecto en descarga
    private val _downloadingProject = kotlinx.coroutines.flow.MutableStateFlow<Project?>(null)
    val downloadingProject: kotlinx.coroutines.flow.StateFlow<Project?> = _downloadingProject

    // Nuevo: callback para avisar a la UI que la descarga terminó
    private val _showInstallPrompt = MutableStateFlow<Project?>(null)
    val showInstallPrompt: StateFlow<Project?> = _showInstallPrompt

    fun startDownload(context: android.content.Context, project: Project, force: Boolean = false) {
        _downloadingProject.value = project
        _downloadProgress.value = 0
        if (force) {
            try { com.just6889.td.tradu_launcher.data.ApkUtils.getApkFile(context, project).delete() } catch (_: Exception) {}
        }
        com.just6889.td.tradu_launcher.data.ApkUtils.downloadApk(
            context,
            project,
            onDownloadComplete = {
                _downloadProgress.value = 100
                _downloadingProject.value = null
                _showInstallPrompt.value = project
                refreshProjectState(context, project)
            },
            onProgress = { progress ->
                _downloadProgress.value = progress
            }
        )
    }

    fun saveInstalledVersion(context: android.content.Context, project: Project) {
        val prefs = context.getSharedPreferences("launcher_versions", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("installed_version_${project.id_proyecto}", project.version).apply()
    }

    fun getInstalledVersion(context: android.content.Context, project: Project): String? {
        val prefs = context.getSharedPreferences("launcher_versions", android.content.Context.MODE_PRIVATE)
        return prefs.getString("installed_version_${project.id_proyecto}", null)
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
            if (context != null) {
                detectInstalledAppsSilently(context, proyectos)
            }
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
        viewModelScope.launch {
            val apkFile = ApkUtils.getApkFile(context, project)
            var realPackageName = project.packageName
            if (apkFile.exists()) {
                ApkUtils.getPackageNameFromApk(context, apkFile)?.let {
                    realPackageName = it
                }
            }
            android.util.Log.d("ProjectsViewModel", "[refreshProjectState] Proyecto: ${project.titulo} | id: ${project.id_proyecto} | packageName usado: $realPackageName")
            val isInstalled = ApkUtils.isApkInstalled(context, realPackageName)
            android.util.Log.d("ProjectsViewModel", "[refreshProjectState] isInstalled($realPackageName): $isInstalled")

            val previousState = _projectInstallStates.value[project.id_proyecto]
            var installedVersion = getInstalledVersion(context, project)

            if (isInstalled) {
                if (_showInstallPrompt.value?.id_proyecto == project.id_proyecto) {
                    clearInstallPrompt()
                }
            }

            if (isInstalled && installedVersion == null) {
                saveInstalledVersion(context, project)
                installedVersion = project.version
                android.util.Log.d("ProjectsViewModel", "[refreshProjectState] Sincronizando versión para app instalada: ${project.id_proyecto} -> ${project.version}")
            } else if (!isInstalled && installedVersion != null) {
                val prefs = context.getSharedPreferences("launcher_versions", android.content.Context.MODE_PRIVATE)
                prefs.edit().remove("installed_version_${project.id_proyecto}").apply()
                installedVersion = null
                android.util.Log.d("ProjectsViewModel", "[refreshProjectState] Limpiando versión para app desinstalada: ${project.id_proyecto}")
            }

            android.util.Log.d("ProjectsViewModel", "[refreshProjectState] installedVersion (launcher): $installedVersion | version (json): ${project.version}")

            val state = when {
                isInstalled && installedVersion != null && project.version != installedVersion -> ProjectInstallState.UPDATE_AVAILABLE
                isInstalled -> ProjectInstallState.INSTALLED
                apkFile.exists() -> ProjectInstallState.DOWNLOADED
                else -> ProjectInstallState.NOT_DOWNLOADED
            }
            val newMap = _projectInstallStates.value.toMutableMap()
            newMap[project.id_proyecto] = state
            _projectInstallStates.value = newMap

            if (state == ProjectInstallState.INSTALLED && (previousState == ProjectInstallState.DOWNLOADED || previousState == ProjectInstallState.UPDATE_AVAILABLE)) {
                val settingsRepository = SettingsRepository(context)
                val shouldDelete = settingsRepository.deleteApkAfterInstall.first()
                if (shouldDelete) {
                    try {
                        if (apkFile.exists()) {
                            apkFile.delete()
                            android.util.Log.d("ProjectsViewModel", "APK borrado automáticamente tras instalación: ${apkFile.name}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ProjectsViewModel", "Error al borrar APK automáticamente", e)
                    }
                }
            }
        }
    }
}