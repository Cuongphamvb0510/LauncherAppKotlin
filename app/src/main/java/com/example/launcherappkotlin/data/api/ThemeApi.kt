package com.example.launcherappkotlin.data.api

import com.example.launcherappkotlin.data.model.ThemeResponse
import retrofit2.http.GET

interface ThemeApi {
    @GET("theme")
    suspend fun getTheme(): ThemeResponse
}