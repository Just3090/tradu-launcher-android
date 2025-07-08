
package com.just6889.td.tradu_launcher

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.Icon

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
import com.just6889.td.tradu_launcher.viewmodel.ProjectsViewModel
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
import com.just6889.td.tradu_launcher.ui.SettingsScreen
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
        setContent {
            TraduLauncherTheme {
                OnboardingGate {
                    val factory = ProjectsViewModelFactory(repository)
                    val viewModel: ProjectsViewModel = viewModel(factory = factory)
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

                    // --- Bottom Navigation ---
                    var selectedTab by remember { mutableStateOf(0) }
                    var showDetailProject by remember { mutableStateOf<Project?>(null) }
                    var backPressCount by remember { mutableStateOf(0) }
                    val context = LocalContext.current
                    val backPressScope = rememberCoroutineScope()

                    AnimatedContent(
                        targetState = showDetailProject,
                        transitionSpec = {
                            if (targetState != null) {
                                // Biblioteca -> Detalles
                                ContentTransform(
                                    targetContentEnter = slideInHorizontally(
                                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                                    ) { width -> width },
                                    initialContentExit = slideOutHorizontally(
                                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                                    ) { width -> -width },
                                    targetContentZIndex = 1f
                                )
                            } else {
                                // Detalles -> Biblioteca
                                ContentTransform(
                                    targetContentEnter = slideInHorizontally(
                                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                                    ) { width -> -width },
                                    initialContentExit = slideOutHorizontally(
                                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                                    ) { width -> width },
                                    targetContentZIndex = 1f
                                )
                            }
                        },
                        label = "Biblioteca-Detalles"
                    ) { detailProject ->
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = {
                                if (detailProject == null) {
                                    NavigationBar {
                                        NavigationBarItem(
                                            selected = selectedTab == 0,
                                            onClick = { selectedTab = 0 },
                                            icon = { Icon(Icons.Filled.Home, contentDescription = "Biblioteca") },
                                            label = { Text("Biblioteca") }
                                        )
                                        NavigationBarItem(
                                            selected = selectedTab == 1,
                                            onClick = { selectedTab = 1 },
                                            icon = { Icon(Icons.Filled.Settings, contentDescription = "Ajustes") },
                                            label = { Text("Ajustes") }
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            if (detailProject == null) {
                                when (selectedTab) {
                                    0 -> {
                                        ProjectsScreen(
                                            viewModel = viewModel,
                                            onProjectClick = { showDetailProject = it },
                                            modifier = Modifier.padding(innerPadding)
                                        )
                                        // Doble back para salir
                                        BackHandler(enabled = selectedTab == 0) {
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
                                    1 -> {
                                        SettingsScreen(modifier = Modifier.padding(innerPadding))
                                        // Back en ajustes regresa a biblioteca
                                        BackHandler(enabled = selectedTab == 1) {
                                            selectedTab = 0
                                        }
                                    }
                                }
                            } else {
                                ProjectDetailScreen(
                                    project = detailProject,
                                    viewModel = viewModel,
                                    onBack = { showDetailProject = null },
                                    modifier = Modifier.padding(innerPadding)
                                )
                                // Back en detalles regresa a biblioteca
                                BackHandler(enabled = true) {
                                    showDetailProject = null
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
