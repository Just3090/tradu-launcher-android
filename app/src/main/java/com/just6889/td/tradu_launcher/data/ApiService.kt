package com.just6889.td.tradu_launcher.data

import retrofit2.http.GET
import retrofit2.http.Url
import retrofit2.Response
import retrofit2.http.POST
import retrofit2.http.Body

@kotlinx.serialization.Serializable
data class LoginRequest(val username: String, val password: String)

@kotlinx.serialization.Serializable
data class LoginResponse(
    val access: String,
    val username: String? = null,
    val avatar_url: String? = null
)

interface ApiService {
    @GET
    suspend fun getProjectsData(@Url url: String): Response<ProjectsApiResponse>

    @POST("api/login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}
