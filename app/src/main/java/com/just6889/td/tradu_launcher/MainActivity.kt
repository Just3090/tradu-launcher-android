package com.just6889.td.tradu_launcher

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
// import androidx.compose.runtime.livedata.observeAsState (no se usa)
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configura Retrofit con kotlinx.serialization
        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://traduction-club.live/")
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory(contentType))
            .build()
        val apiService = retrofit.create(ApiService::class.java)
        val repository = ProjectRepository(apiService, applicationContext)

        setContent {
            TraduLauncherTheme {
                com.just6889.td.tradu_launcher.ui.OnboardingGate {
                    val factory = com.just6889.td.tradu_launcher.viewmodel.ProjectsViewModelFactory(repository)
                    val viewModel: ProjectsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        ProjectsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

// ...