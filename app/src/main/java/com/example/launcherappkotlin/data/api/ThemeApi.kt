package com.example.launcherappkotlin.data.api

import com.example.launcherappkotlin.data.model.ThemeResponse
import retrofit2.http.GET
import retrofit2.http.Headers

/**
 * Contract gọi backend theme.
 * Full URL = BuildConfig.BASE_URL + path → vd. http://127.0.0.1:8080/api/v1/theme
 */
interface ThemeApi {

    @Headers("Accept: application/json")
    @GET("api/v1/theme")
    suspend fun getTheme(): ThemeResponse
}
