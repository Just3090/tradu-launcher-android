package com.just6889.td.tradu_launcher.ui

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun OnboardingGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var onboardingComplete by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        onboardingComplete = isOnboardingComplete(context)
    }

    when (onboardingComplete) {
        null -> {}
        false -> WelcomeScreen(onContinue = {
            onboardingComplete = true
        })
        true -> content()
    }
}
