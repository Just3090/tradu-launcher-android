package com.just6889.td.tradu_launcher.data

import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val id_proyecto: String,
    val titulo: String,
    val descripcion: String,
    val imagen_portada_url: String,
    val url_descarga_apk: String,
    val version: String,
    val packageName: String,
    val detalles: ProjectDetails? = null
)

@Serializable
data class ProjectDetails(
    val descripcion_full: String? = null,
    val genero: String? = null,
    val autor: String? = null,
    val traducido_por: String? = null,
    val port_hecho_por: String? = null
)

@Serializable
data class ProjectsApiResponse(
    val version_catalogo: String,
    val proyectos: List<Project>
)
