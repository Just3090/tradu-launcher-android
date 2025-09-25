package com.just6889.td.tradu_launcher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just6889.td.tradu_launcher.data.ApiService
import com.just6889.td.tradu_launcher.data.AuthRepository
import com.just6889.td.tradu_launcher.viewmodel.LoginViewModel
import com.just6889.td.tradu_launcher.viewmodel.LoginViewModelFactory

@Composable
fun AuthGate(apiService: ApiService, content: @Composable (AuthRepository) -> Unit) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context, apiService) }
    val token by authRepository.authToken.collectAsState(initial = "loading")
    var showHelp by remember { mutableStateOf(false) }

    if (showHelp) {
        LoginHelpScreen(onBack = { showHelp = false })
        return
    }

    when (token) {
        "loading" -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        null, "" -> {
            val factory = LoginViewModelFactory(authRepository)
            val loginViewModel: LoginViewModel = viewModel(factory = factory)
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {},
                onHelpClick = { showHelp = true }
            )
        }
        else -> {
            content(authRepository)
        }
    }
}