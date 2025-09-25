package com.just6889.td.tradu_launcher.ui

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.res.painterResource
import com.just6889.td.tradu_launcher.R
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.just6889.td.tradu_launcher.viewmodel.ProjectsViewModel
import com.just6889.td.tradu_launcher.data.Project
import com.just6889.td.tradu_launcher.data.ApkUtils
import com.just6889.td.tradu_launcher.data.ProjectInstallState
import android.os.Build
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect

@Composable
fun ProjectsScreen(
    viewModel: ProjectsViewModel,
    onProjectClick: (Project) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    // --- Permiso de notificaciones (Android 13+) ---
    val showNotificationDialog = rememberSaveable { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "No se podrán mostrar notificaciones de descargas.", Toast.LENGTH_LONG).show()
        }
    }
    // --- Diálogo emergente de descarga completada ---
    val showInstallPrompt by viewModel.showInstallPrompt.collectAsState()
    val showDialog = remember { mutableStateOf(false) }
    LaunchedEffect(showInstallPrompt) {
        showDialog.value = showInstallPrompt != null
    }
    if (showDialog.value && showInstallPrompt != null) {
        AlertDialog(
            onDismissRequest = {
                showDialog.value = false
                viewModel.clearInstallPrompt()
            },
            title = { Text("Descarga completada") },
            text = { Text("El APK de ${showInstallPrompt!!.titulo} se descargó correctamente. ¿Deseas instalarlo ahora?") },
            confirmButton = {
                TextButton(onClick = {
                    val projectToInstall = showInstallPrompt!!
                    ApkUtils.installApk(context, projectToInstall)
                    viewModel.saveInstalledVersion(context, projectToInstall)
                    showDialog.value = false
                    viewModel.clearInstallPrompt()
                    viewModel.refreshProjectState(context, projectToInstall)
                }) {
                    Text("Instalar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog.value = false
                    viewModel.clearInstallPrompt()
                }) {
                    Text("Cerrar")
                }
            }
        )
    }
    SideEffect {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (!notificationManager.areNotificationsEnabled()) {
                showNotificationDialog.value = true
            }
        }
    }
    if (showNotificationDialog.value) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showNotificationDialog.value = false },
            title = { androidx.compose.material3.Text("Permitir notificaciones") },
            text = { androidx.compose.material3.Text("¿Quieres permitir notificaciones para gestionar descargas e instalaciones?") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showNotificationDialog.value = false
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }) {
                    androidx.compose.material3.Text("Permitir")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showNotificationDialog.value = false }) {
                    androidx.compose.material3.Text("No, gracias")
                }
            }
        )
    }
    LaunchedEffect(projects) {
        if (projects.isNotEmpty()) {
            viewModel.detectInstalledAppsSilently(context, projects)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues())
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }
            projects.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.no_projects_found),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = modifier.fillMaxSize().padding(8.dp)
                ) {
                    items(projects) { project ->
                        ProjectCard(project, context, viewModel) {
                            onProjectClick(it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    context: android.content.Context,
    viewModel: ProjectsViewModel,
    onShowDetail: (Project) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = project.imagen_portada_url,
                    placeholder = painterResource(id = R.drawable.placeholder_image),
                    error = painterResource(id = R.drawable.error_image)
                ),
                contentDescription = project.titulo,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(project.titulo, style = MaterialTheme.typography.titleMedium)
                Text(project.descripcion, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onShowDetail(project) }) {
                Text("Ver")
            }
        }
    }
}

@Composable
fun InstallOrOpenButton(project: Project, context: android.content.Context, viewModel: ProjectsViewModel) {
    val downloadingProject by viewModel.downloadingProject.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val projectInstallStates by viewModel.projectInstallStates.collectAsState()
    val state = projectInstallStates[project.id_proyecto] ?: ProjectInstallState.NOT_DOWNLOADED

    val errorMessage = remember { mutableStateOf<String?>(null) }

    if (downloadingProject?.id_proyecto == project.id_proyecto) {
        // Mostrar barra de progreso
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.CircularProgressIndicator(progress = downloadProgress / 100f)
            Text("Descargando... $downloadProgress%", style = MaterialTheme.typography.bodySmall)
            errorMessage.value?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    } else {
        when (state) {
            ProjectInstallState.NOT_DOWNLOADED -> {
                Button(onClick = {
                    try {
                        viewModel.startDownload(context, project)
                        errorMessage.value = null
                    } catch (e: Exception) {
                        errorMessage.value = "Error al iniciar descarga: ${e.localizedMessage}"
                    }
                }) {
                    Text(stringResource(id = R.string.download))
                }
                errorMessage.value?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            ProjectInstallState.DOWNLOADED -> {
                Button(onClick = {
                    try {
                        ApkUtils.installApk(context, project)
                        // Guardar versión instalada (del JSON) al lanzar la instalación
                        viewModel.saveInstalledVersion(context, project)
                        // Refrescar estado tras intentar instalar
                        viewModel.refreshProjectState(context, project)
                        errorMessage.value = null
                    } catch (e: Exception) {
                        errorMessage.value = "Error al instalar: ${e.localizedMessage}"
                    }
                }) {
                    Text(stringResource(id = R.string.install))
                }
                errorMessage.value?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            ProjectInstallState.UPDATE_AVAILABLE -> {
                Button(onClick = {
                    try {
                        viewModel.startDownload(context, project)
                        errorMessage.value = null
                    } catch (e: Exception) {
                        errorMessage.value = "Error al actualizar: ${e.localizedMessage}"
                    }
                }) {
                    Text("Actualizar")
                }
                errorMessage.value?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            ProjectInstallState.INSTALLED -> {
                Button(onClick = {
                    try {
                        // Detectar packageName real desde el APK si existe
                        val apkFile = ApkUtils.getApkFile(context, project)
                        var realPackageName = project.packageName
                        if (apkFile.exists()) {
                            ApkUtils.getPackageNameFromApk(context, apkFile)?.let {
                                realPackageName = it
                            }
                        }
                        // Antes de abrir, borrar el APK si existe y la app está instalada
                        val deleted = try { ApkUtils.deleteApkIfInstalled(context, project) } catch (e: Exception) {
                            errorMessage.value = "Error al borrar APK: ${e.localizedMessage}"
                            false
                        }
                        var launchIntent = context.packageManager.getLaunchIntentForPackage(realPackageName)
                        if (launchIntent == null) {
                            launchIntent = Intent(Intent.ACTION_MAIN).also {
                                it.addCategory(Intent.CATEGORY_LAUNCHER)
                                it.setPackage(realPackageName)
                                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        }

                        if (launchIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(launchIntent)
                            errorMessage.value = null
                            // Refrescar estado tras borrar
                            viewModel.refreshProjectState(context, project)
                        } else {
                            errorMessage.value = context.getString(R.string.cannot_open_app)
                            viewModel.detectInstalledAppsSilently(context, listOf(project))
                        }
                    } catch (e: Exception) {
                        errorMessage.value = "Error al abrir/jugar: ${e.localizedMessage}"
                    }
                }) {
                    Text(stringResource(id = R.string.play))
                }
                errorMessage.value?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}