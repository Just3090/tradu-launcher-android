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
    /**
     * Detección silenciosa y robusta de apps instaladas al cargar proyectos o volver al foreground.
     * Si una app está instalada y no hay versión local, la marca como instalada (versión del JSON).
     * Si una app NO está instalada pero hay versión local, la borra (refuerzo extra).
     */
    fun detectInstalledAppsSilently(context: android.content.Context, projects: List<Project>) {
        val prefs = context.getSharedPreferences("launcher_versions", android.content.Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val pm = context.packageManager
        for (project in projects) {
            val isInstalled = ApkUtils.isApkInstalled(context, project.packageName)
            val localVersion = prefs.getString("installed_version_${project.id_proyecto}", null)
            if (isInstalled && localVersion == null) {
                editor.putString("installed_version_${project.id_proyecto}", project.version)
                android.util.Log.d("ProjectsViewModel", "[detectInstalledAppsSilently] App instalada externamente detectada: ${project.packageName} (id: ${project.id_proyecto}) -> se marca como INSTALLED y version=${project.version}")
            } else if (!isInstalled && localVersion != null) {
                editor.remove("installed_version_${project.id_proyecto}")
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
                    var installedVersion: String? = null
                    if (isInstalled) {
                        try {
                            val pm = context.packageManager
                            val pkgInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                pm.getPackageInfo(realPackageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                            } else {
                                @Suppress("DEPRECATION")
                                pm.getPackageInfo(realPackageName, 0)
                            }
                            installedVersion = pkgInfo.versionName
                        } catch (_: Exception) {}
                    }
                    val state = when {
                        isInstalled && installedVersion != null && installedVersion != project.version -> ProjectInstallState.UPDATE_AVAILABLE
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
        android.util.Log.d("ProjectsViewModel", "[refreshProjectState] Proyecto: ${project.titulo} | id: ${project.id_proyecto} | packageName usado: $realPackageName")
        val isInstalled = ApkUtils.isApkInstalled(context, realPackageName)
        android.util.Log.d("ProjectsViewModel", "[refreshProjectState] isInstalled($realPackageName): $isInstalled")

        // NUEVO: versión instalada según el launcher (no la del APK)
        val installedVersion = getInstalledVersion(context, project)
        android.util.Log.d("ProjectsViewModel", "[refreshProjectState] installedVersion (launcher): $installedVersion | version (json): ${project.version}")

        val state = when {
            isInstalled && installedVersion != null && installedVersion != project.version -> ProjectInstallState.UPDATE_AVAILABLE
            isInstalled && installedVersion == project.version -> ProjectInstallState.INSTALLED
            apkFile.exists() -> ProjectInstallState.DOWNLOADED
            else -> ProjectInstallState.NOT_DOWNLOADED
        }
        val newMap = _projectInstallStates.value.toMutableMap()
        newMap[project.id_proyecto] = state
        _projectInstallStates.value = newMap

        if (apkFile.exists() && !isInstalled) {
            val prevState = _projectInstallStates.value[project.id_proyecto]
            if (prevState != ProjectInstallState.DOWNLOADED) {
                android.widget.Toast.makeText(context, "No se detectó la instalación del paquete: $realPackageName", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}
