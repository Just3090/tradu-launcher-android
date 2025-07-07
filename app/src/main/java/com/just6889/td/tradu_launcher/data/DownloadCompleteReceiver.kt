package com.just6889.td.tradu_launcher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.DownloadManager
import android.util.Log
import android.widget.Toast

class DownloadCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("DownloadCompleteReceiver", "BroadcastReceiver.onReceive() llamado")
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        Log.d("DownloadCompleteReceiver", "Intent recibido con EXTRA_DOWNLOAD_ID=$id")
        val prefs = context.getSharedPreferences("apk_downloads", Context.MODE_PRIVATE)
        val projectJson = prefs.getString(id.toString(), null)
        if (projectJson != null) {
            try {
                val project = kotlinx.serialization.json.Json.decodeFromString(com.just6889.td.tradu_launcher.data.Project.serializer(), projectJson)
                // Crear intent para instalar el APK
                val apkFile = ApkUtils.getApkFile(context, project)
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        androidx.core.content.FileProvider.getUriForFile(
                            context,
                            context.packageName + ".provider",
                            apkFile
                        ),
                        "application/vnd.android.package-archive"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                NotificationUtils.showApkDownloadedNotification(context, project, installIntent)
                Toast.makeText(context, "Descarga de ${project.titulo} completada", Toast.LENGTH_SHORT).show()
                Log.d("DownloadCompleteReceiver", "Notificación personalizada mostrada para ${project.titulo}")
                // Limpiar relación
                prefs.edit().remove(id.toString()).apply()
            } catch (e: Exception) {
                Log.e("DownloadCompleteReceiver", "Error al decodificar Project desde SharedPreferences", e)
                NotificationUtils.showSimpleNotification(context, "Descarga completada", "La descarga ha finalizado. Puedes instalar el APK desde la barra de notificaciones.")
            }
        } else {
            Toast.makeText(context, "Descarga completada", Toast.LENGTH_SHORT).show()
            NotificationUtils.showSimpleNotification(context, "Descarga completada", "La descarga ha finalizado. Puedes instalar el APK desde la barra de notificaciones.")
        }
    }
}
