package com.just6889.td.tradu_launcher.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginHelpScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Ayuda de Inicio de Sesión") },
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
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("¿No tienes una cuenta?\n\nPuedes crear una cuenta nueva en nuestro sitio web:", style = MaterialTheme.typography.titleLarge)
            val registerText = buildAnnotatedString {
                append("")
                pushStringAnnotation(tag = "URL", annotation = "https://traduction-club.live/")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                    append("https://traduction-club.live/")
                }
                pop()
            }
            ClickableText(
                text = registerText,
                style = MaterialTheme.typography.bodyLarge,
                onClick = { offset ->
                    registerText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { openUrl(it.item) }
                }
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text("¿Creaste tu cuenta con Discord?", style = MaterialTheme.typography.titleLarge)
            Text(
                "Si te registraste en la web usando tu cuenta de Discord, necesitas generar una contraseña para usar el launcher.\n\n1. Ve a la página de reseteo de contraseña:",
                style = MaterialTheme.typography.bodyLarge
            )

            val passwordResetText = buildAnnotatedString {
                append("")
                pushStringAnnotation(tag = "URL", annotation = "https://traduction-club.live/password-reset/")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                    append("https://traduction-club.live/password-reset/")
                }
                pop()
            }
            ClickableText(
                text = passwordResetText,
                style = MaterialTheme.typography.bodyLarge,
                onClick = { offset ->
                    passwordResetText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { openUrl(it.item) }
                }
            )

            Text(
                "2. Usa el correo electrónico asociado a tu cuenta de Discord para solicitar el cambio.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                "3. Una vez establecida tu nueva contraseña, úsala para iniciar sesión aquí.",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Nota: El inicio de sesión nativo con Discord (OAuth) aún no está implementado en el launcher. Este paso es necesario por ahora.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}