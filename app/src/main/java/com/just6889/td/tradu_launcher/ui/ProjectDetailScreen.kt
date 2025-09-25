@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.just6889.td.tradu_launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.just6889.td.tradu_launcher.data.Project
import com.just6889.td.tradu_launcher.viewmodel.ProjectsViewModel
import com.just6889.td.tradu_launcher.data.ApkUtils
import com.just6889.td.tradu_launcher.data.ProjectInstallState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import com.just6889.td.tradu_launcher.R
import com.just6889.td.tradu_launcher.ui.InstallOrOpenButton
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun ProjectDetailScreen(
    project: Project,
    viewModel: ProjectsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val projectInstallStates by viewModel.projectInstallStates.collectAsState()
    val state = projectInstallStates[project.id_proyecto] ?: ProjectInstallState.NOT_DOWNLOADED

    val detalles = project.detalles
    android.util.Log.d("ProjectDetailScreen", "Abriendo detalles de: ${project.titulo} | id: ${project.id_proyecto} | estado: $state")

    // Manejar prompt de instalación y refresco de estado en detalles
    // val showInstallPrompt by viewModel.showInstallPrompt.collectAsState()
    // LaunchedEffect(showInstallPrompt) {
    //     if (showInstallPrompt?.id_proyecto == project.id_proyecto) {
    //         viewModel.refreshProjectState(context, project)
    //         android.widget.Toast.makeText(context, "El APK de ${project.titulo} se descargó correctamente. ¿Deseas instalarlo ahora?", android.widget.Toast.LENGTH_LONG).show()
    //         viewModel.clearInstallPrompt()
    //     }
    // }

    // Refresca el estado del proyecto cada vez que la pantalla vuelve al frente
    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner, project) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshProjectState(context, project)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(project.titulo) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .padding(WindowInsets.navigationBars.asPaddingValues()),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InstallOrOpenButton(project, context, viewModel)
                // Solo botón Desinstalar, el de Actualizar se gestiona en InstallOrOpenButton
                var showDialog = remember { mutableStateOf(false) }
                if (showDialog.value) {
                    AlertDialog(
                        onDismissRequest = { showDialog.value = false },
                        title = { Text("Función en desarrollo") },
                        text = { Text("La desinstalación automática está en desarrollo. Por favor, desinstala la app manualmente desde los ajustes de Android.") },
                        confirmButton = {
                            TextButton(onClick = {
                                showDialog.value = false
                            }) { Text("Vale") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showDialog.value = false
                                // Abrir ajustes de la app específica
                                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = android.net.Uri.parse("package:" + project.packageName)
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "No se pudo abrir ajustes de la app", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }) { Text("Abrir ajustes de la app") }
                        }
                    )
                }
                Button(
                    onClick = { showDialog.value = true },
                    enabled = state == ProjectInstallState.INSTALLED || state == ProjectInstallState.UPDATE_AVAILABLE
                ) {
                    Text("Desinstalar")
                }
            }
        }
    ) { innerPadding ->
        val scrollState = androidx.compose.foundation.rememberScrollState()
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = project.imagen_portada_url,
                    placeholder = painterResource(id = R.drawable.placeholder_image),
                    error = painterResource(id = R.drawable.error_image)
                ),
                contentDescription = project.titulo,
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(project.titulo, style = MaterialTheme.typography.headlineSmall)
            detalles?.descripcion_full?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))
            detalles?.genero?.let {
                Text("Género: $it", style = MaterialTheme.typography.bodySmall)
            }
            detalles?.autor?.let {
                Text("Autor: $it", style = MaterialTheme.typography.bodySmall)
            }
            detalles?.traducido_por?.let {
                Text("Traducido por: $it", style = MaterialTheme.typography.bodySmall)
            }
            detalles?.port_hecho_por?.let {
                Text("Port hecho por: $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
