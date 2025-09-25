package com.just6889.td.tradu_launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.just6889.td.tradu_launcher.R
import com.just6889.td.tradu_launcher.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val username by viewModel.username.collectAsState(initial = null)
    val avatarUrl by viewModel.avatarUrl.collectAsState(initial = null)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Mi Tradu-Cuenta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Image(
                painter = rememberAsyncImagePainter(
                    model = avatarUrl,
                    placeholder = painterResource(id = R.drawable.placeholder_image),
                    error = painterResource(id = R.drawable.error_image)
                ),
                contentDescription = "Avatar de usuario",
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(username ?: "Cargando...", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    viewModel.logout()
                    onLogout()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Cerrar sesión")
            }
        }
    }
}