package com.just6889.td.tradu_launcher.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "onboarding")

@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var notificationGranted by remember { mutableStateOf(false) }
    var installUnknownAllowed by remember { mutableStateOf(false) }

    // Launcher para permiso de notificaciones
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationGranted = granted
    }

    fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationGranted = nm.areNotificationsEnabled()
        } else {
            notificationGranted = true
        }
        installUnknownAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        checkPermissions()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("¡Bienvenido a Tradu-Launcher!", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Para poder descargar e instalar juegos, necesitamos los siguientes permisos:", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                enabled = !notificationGranted
            ) {
                Text(if (notificationGranted) "Notificaciones permitidas" else "Permitir notificaciones")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:" + context.packageName)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                },
                enabled = !installUnknownAllowed
            ) {
                Text(if (installUnknownAllowed) "Permiso de instalar APKs concedido" else "Permitir instalación de APKs")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    // Guardar flag de onboarding completado
                    scope.launch {
                        context.dataStore.edit { prefs ->
                            prefs[booleanPreferencesKey("onboarding_complete")] = true
                        }
                        onContinue()
                    }
                },
                enabled = notificationGranted && installUnknownAllowed
            ) {
                Text("Continuar")
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Si ya diste los permisos pero la app no lo detecta, cierra y vuelve a abrir la app. Si el problema persiste, reinicia la aplicación completamente (forzar cierre y abrir de nuevo).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

suspend fun isOnboardingComplete(context: Context): Boolean {
    val prefs = context.dataStore.data.firstOrNull()
    return prefs?.get(booleanPreferencesKey("onboarding_complete")) == true
}
