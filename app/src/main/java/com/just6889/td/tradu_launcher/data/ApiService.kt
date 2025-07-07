package com.just6889.td.tradu_launcher.data

import retrofit2.http.GET
import retrofit2.http.Url
import retrofit2.Response

interface ApiService {
    @GET
    suspend fun getProjectsData(@Url url: String): Response<ProjectsApiResponse>
}
