package com.just6889.td.tradu_launcher

import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.DisposableEffect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.just6889.td.tradu_launcher.ui.theme.TraduLauncherTheme
import com.just6889.td.tradu_launcher.data.ApiService
import com.just6889.td.tradu_launcher.data.ProjectRepository
import com.just6889.td.tradu_launcher.data.SettingsRepository
import com.just6889.td.tradu_launcher.ui.SettingsScreen
import com.just6889.td.tradu_launcher.viewmodel.ProjectsViewModel
import com.just6889.td.tradu_launcher.viewmodel.SettingsViewModel
import com.just6889.td.tradu_launcher.viewmodel.SettingsViewModelFactory
import com.just6889.td.tradu_launcher.ui.ProjectsScreen
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.just6889.td.tradu_launcher.data.Project
import com.just6889.td.tradu_launcher.ui.OnboardingGate
import com.just6889.td.tradu_launcher.ui.ProjectDetailScreen
import com.just6889.td.tradu_launcher.viewmodel.ProjectsViewModelFactory
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.just6889.td.tradu_launcher.ui.AuthGate
import com.just6889.td.tradu_launcher.ui.AccountScreen
import com.just6889.td.tradu_launcher.ui.LoginHelpScreen

private enum class Screen {
    Projects, Settings, ProjectDetail, Account, LoginHelp
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://traduction-club.live/")
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory(contentType))
            .build()
        val apiService = retrofit.create(ApiService::class.java)
        val repository = ProjectRepository(apiService, applicationContext)
        val settingsRepository = SettingsRepository(applicationContext)
        setContent {
            TraduLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuthGate(apiService = apiService) { authRepository ->
                        var showLoginHelp by remember { mutableStateOf(false) }

                        if (showLoginHelp) {
                            LoginHelpScreen(onBack = { showLoginHelp = false })
                        } else {
                            OnboardingGate {
                                val factory = ProjectsViewModelFactory(repository)
                                val settingsFactory = SettingsViewModelFactory(settingsRepository, authRepository)
                                val viewModel: ProjectsViewModel = viewModel(factory = factory)
                                val settingsViewModel: SettingsViewModel = viewModel(factory = settingsFactory)
                                val mainScope = MainScope()

                                val observer = LifecycleEventObserver { _, event ->
                                    if (event == Lifecycle.Event.ON_RESUME) {
                                        mainScope.launch {
                                            val projects = viewModel.projects.value
                                            if (projects.isNotEmpty()) {
                                                viewModel.detectInstalledAppsSilently(this@MainActivity, projects)
                                            }
                                        }
                                    }
                                }

                                DisposableEffect(Unit) {
                                    val lifecycle = ProcessLifecycleOwner.get().lifecycle
                                    lifecycle.addObserver(observer)
                                    onDispose { lifecycle.removeObserver(observer) }
                                }

                                var currentScreen by remember { mutableStateOf(Screen.Projects) }
                                var selectedProject by remember { mutableStateOf<Project?>(null) }
                                var backPressCount by remember { mutableStateOf(0) }
                                val context = LocalContext.current
                                val backPressScope = rememberCoroutineScope()

                                Scaffold(
                                    modifier = Modifier.fillMaxSize(),
                                    bottomBar = {
                                        if (currentScreen == Screen.Projects || currentScreen == Screen.Settings) {
                                            NavigationBar {
                                                NavigationBarItem(
                                                    selected = currentScreen == Screen.Projects,
                                                    onClick = { currentScreen = Screen.Projects },
                                                    icon = { Icon(Icons.Filled.Home, contentDescription = "Biblioteca") },
                                                    label = { Text("Biblioteca") }
                                                )
                                                NavigationBarItem(
                                                    selected = currentScreen == Screen.Settings,
                                                    onClick = { currentScreen = Screen.Settings },
                                                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Ajustes") },
                                                    label = { Text("Ajustes") }
                                                )
                                            }
                                        }
                                    }
                                ) { innerPadding ->
                                    AnimatedContent(
                                        targetState = currentScreen,
                                        transitionSpec = {
                                            if (targetState.ordinal > initialState.ordinal) {
                                                slideInHorizontally { width -> width } togetherWith
                                                        slideOutHorizontally { width -> -width }
                                            } else {
                                                slideInHorizontally { width -> -width } togetherWith
                                                        slideOutHorizontally { width -> width }
                                            }
                                        },
                                        label = "MainScreenAnimation"
                                    ) { screen ->
                                        when (screen) {
                                            Screen.Projects -> {
                                                ProjectsScreen(
                                                    viewModel = viewModel,
                                                    onProjectClick = { project ->
                                                        selectedProject = project
                                                        currentScreen = Screen.ProjectDetail
                                                    },
                                                    modifier = Modifier.padding(innerPadding)
                                                )
                                                BackHandler(enabled = true) {
                                                    if (backPressCount == 0) {
                                                        backPressCount = 1
                                                        Toast.makeText(context, "Pulsa atrás una vez más para salir", Toast.LENGTH_SHORT).show()
                                                        backPressScope.launch {
                                                            delay(2000)
                                                            backPressCount = 0
                                                        }
                                                    } else {
                                                        finish()
                                                    }
                                                }
                                            }
                                            Screen.Settings -> {
                                                SettingsScreen(
                                                    modifier = Modifier.padding(innerPadding),
                                                    authRepository = authRepository,
                                                    onAccountClick = { currentScreen = Screen.Account }
                                                )
                                                BackHandler(enabled = true) { currentScreen = Screen.Projects }
                                            }
                                            Screen.ProjectDetail -> {
                                                ProjectDetailScreen(
                                                    project = selectedProject!!,
                                                    viewModel = viewModel,
                                                    onBack = { currentScreen = Screen.Projects },
                                                    modifier = Modifier.padding(innerPadding)
                                                )
                                                BackHandler(enabled = true) { currentScreen = Screen.Projects }
                                            }
                                            Screen.Account -> {
                                                AccountScreen(
                                                    viewModel = settingsViewModel,
                                                    onBack = { currentScreen = Screen.Settings },
                                                    onLogout = {
                                                        val intent = Intent(context, MainActivity::class.java)
                                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                        context.startActivity(intent)
                                                    },
                                                    modifier = Modifier.padding(innerPadding)
                                                )
                                                BackHandler(enabled = true) { currentScreen = Screen.Settings }
                                            }
                                            Screen.LoginHelp -> {
                                                // No-op
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
