package com.just6889.td.tradu_launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.just6889.td.tradu_launcher.R
import com.just6889.td.tradu_launcher.data.ApiService
import com.just6889.td.tradu_launcher.data.AuthRepository
import com.just6889.td.tradu_launcher.data.SettingsRepository
import com.just6889.td.tradu_launcher.viewmodel.SettingsViewModel
import com.just6889.td.tradu_launcher.viewmodel.SettingsViewModelFactory
import java.text.DecimalFormat

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    authRepository: AuthRepository,
    onAccountClick: () -> Unit
) {
    val context = LocalContext.current
    val factory = SettingsViewModelFactory(SettingsRepository(context), authRepository)
    val viewModel: SettingsViewModel = viewModel(factory = factory)

    val deleteApkAfterInstall by viewModel.deleteApkAfterInstall.collectAsState()
    val cacheSize by viewModel.cacheSize.collectAsState()
    val username by viewModel.username.collectAsState(initial = null)
    val avatarUrl by viewModel.avatarUrl.collectAsState(initial = null)

    LaunchedEffect(Unit) {
        viewModel.calculateCacheSize(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // --- Sección de Usuario ---
        if (username == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAccountClick() },
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = avatarUrl,
                            placeholder = painterResource(id = R.drawable.placeholder_image),
                            error = painterResource(id = R.drawable.error_image)
                        ),
                        contentDescription = "Avatar de usuario",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(username ?: "Cargando...", style = MaterialTheme.typography.titleLarge)
                        Text("Ver perfil", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Ajustes de la App ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Borrar APK tras instalar", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Libera espacio automáticamente después de una instalación exitosa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = deleteApkAfterInstall,
                onCheckedChange = { viewModel.setDeleteApkAfterInstall(it) }
            )
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text("Gestión de caché", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Espacio usado por APKs descargados: ${formatFileSize(cacheSize)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.clearCache(context) },
                enabled = cacheSize > 0
            ) {
                Text("Limpiar archivos APK descargados")
            }
        }

    }
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}
