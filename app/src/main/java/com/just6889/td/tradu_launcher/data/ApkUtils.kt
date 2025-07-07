package com.just6889.td.tradu_launcher.data

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import java.io.File
import androidx.core.content.FileProvider
import android.Manifest
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.widget.Toast

object ApkUtils {
    /**
     * Extrae el packageName real de un archivo APK local.
     * Devuelve null si no se puede leer.
     */
    fun getPackageNameFromApk(context: Context, apkFile: File): String? {
        if (!apkFile.exists()) return null
        val pm = context.packageManager
        val info = pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
        return info?.packageName
    }
    /**
     * Borra el archivo APK si la app está instalada. Devuelve true si se borró, false si no existía, lanza excepción si falla.
     */
    fun deleteApkIfInstalled(context: Context, project: Project): Boolean {
        val apkFile = getApkFile(context, project)
        val isInstalled = isApkInstalled(context, project.packageName)
        if (isInstalled && apkFile.exists()) {
            val deleted = apkFile.delete()
            if (!deleted) throw Exception("No se pudo borrar el APK descargado: ${apkFile.absolutePath}")
            return true
        }
        return false
    }
    fun isApkInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getApkFile(context: Context, project: Project): File {
        val fileName = "${project.id_proyecto}_${project.version}.apk"
        return File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
    }

    fun downloadApk(
        context: Context,
        project: Project,
        onDownloadComplete: ((Boolean) -> Unit)? = null,
        onProgress: ((Int) -> Unit)? = null
    ) {
        val apkFile = getApkFile(context, project)
        android.util.Log.d("ApkUtils", "Iniciando descarga de APK: ${apkFile.absolutePath}")
        val request = DownloadManager.Request(Uri.parse(project.url_descarga_apk))
            .setTitle(project.titulo)
            .setDescription("Descargando APK de ${project.titulo}")
            .setDestinationUri(Uri.fromFile(apkFile))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)
        android.util.Log.d("ApkUtils", "DownloadManager.enqueue() -> downloadId=$downloadId")

        // Guardar relación downloadId <-> Project en SharedPreferences
        val prefs = context.getSharedPreferences("apk_downloads", Context.MODE_PRIVATE)
        prefs.edit().putString(downloadId.toString(), kotlinx.serialization.json.Json.encodeToString(Project.serializer(), project)).apply()
        android.util.Log.d("ApkUtils", "Relación downloadId <-> Project guardada en SharedPreferences")

        // Progreso: consultar periódicamente
        var handler: android.os.Handler? = null
        var progressRunnable: Runnable? = null
        if (onProgress != null) {
            handler = android.os.Handler(android.os.Looper.getMainLooper())
            val query = DownloadManager.Query().setFilterById(downloadId)
            progressRunnable = object : Runnable {
                override fun run() {
                    val cursor = manager.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        val progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                        onProgress(progress)
                        android.util.Log.d("ApkUtils", "Progreso descarga: $progress% (status=$status)")
                        if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                            cursor.close()
                            handler?.removeCallbacks(this)
                            android.util.Log.d("ApkUtils", "Descarga finalizada con status=$status, deteniendo handler de progreso")
                            // Notificar a la UI que terminó
                            onDownloadComplete?.invoke(true)
                            return
                        }
                    }
                    cursor?.close()
                    handler?.postDelayed(this, 300)
                }
            }
            handler.post(progressRunnable)
        }
    }

    fun installApk(context: Context, project: Project) {
        val apkFile = getApkFile(context, project)
        val apkUri = FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            apkFile
        )
        android.util.Log.d("ApkUtils", "Intentando instalar APK: ${apkFile.absolutePath}")
        // Verificar permiso para instalar apps desconocidas (Android 8+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val canInstall = context.packageManager.canRequestPackageInstalls()
            android.util.Log.d("ApkUtils", "canRequestPackageInstalls: $canInstall")
            if (!canInstall) {
                Toast.makeText(context, "Debes habilitar 'Permitir desde esta fuente' para instalar APKs", Toast.LENGTH_LONG).show()
                android.util.Log.w("ApkUtils", "No tiene permiso para instalar apps desconocidas. Abriendo settings...")
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:" + context.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            }
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        android.util.Log.d("ApkUtils", "Lanzando intent de instalación de APK")
        try {
            context.startActivity(intent)
            android.util.Log.d("ApkUtils", "Intent de instalación lanzado correctamente")
        } catch (e: Exception) {
            android.util.Log.e("ApkUtils", "Error al lanzar intent de instalación", e)
            Toast.makeText(context, "Error al intentar instalar el APK: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            throw e
        }
    }
}
