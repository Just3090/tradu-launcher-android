package com.just6889.td.tradu_launcher.data

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class ProjectRepository(
    private val apiService: ApiService,
    private val context: Context
) {
    companion object {
        const val JSON_URL = "https://traduction-club.live/api/androidapp/proyectos_android.json"
        const val LOCAL_JSON_FILE = "proyectos_cache.json"
    }

    suspend fun loadProjectsData(): ProjectsApiResponse? {
        try {
            val response = apiService.getProjectsData(JSON_URL)
            android.util.Log.d("ProjectRepository", "HTTP response: ${'$'}{response.code()} ${'$'}{response.message()}")
            if (response.isSuccessful) {
                val data = response.body()
                android.util.Log.d("ProjectRepository", "JSON recibido: ${'$'}{data}")
                if (data != null) {
                    saveJsonLocal(data)
                    return data
                } else {
                    android.util.Log.e("ProjectRepository", "El body del JSON es null")
                }
            } else {
                android.util.Log.e("ProjectRepository", "Error HTTP: ${'$'}{response.code()} - ${'$'}{response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            android.util.Log.e("ProjectRepository", "Excepción al descargar JSON", e)
        }
        val local = loadJsonLocal()
        android.util.Log.d("ProjectRepository", "Cargando JSON local: ${'$'}{local}")
        return local
    }

    private fun saveJsonLocal(data: ProjectsApiResponse) {
        val file = File(context.filesDir, LOCAL_JSON_FILE)
        try {
            file.writeText(Json.encodeToString(data))
            android.util.Log.d("ProjectRepository", "JSON guardado localmente en ${'$'}{file.absolutePath}")
        } catch (e: Exception) {
            android.util.Log.e("ProjectRepository", "Error guardando JSON local", e)
        }
    }

    private fun loadJsonLocal(): ProjectsApiResponse? {
        val file = File(context.filesDir, LOCAL_JSON_FILE)
        if (!file.exists()) {
            android.util.Log.w("ProjectRepository", "Archivo local de proyectos no existe: ${'$'}{file.absolutePath}")
            return null
        }
        return try {
            val text = file.readText()
            android.util.Log.d("ProjectRepository", "Leyendo JSON local: ${'$'}text")
            Json.decodeFromString<ProjectsApiResponse>(text)
        } catch (e: Exception) {
            android.util.Log.e("ProjectRepository", "Error parseando JSON local", e)
            null
        }
    }
}
