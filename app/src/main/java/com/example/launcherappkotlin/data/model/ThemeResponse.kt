package com.example.launcherappkotlin.data.model

import com.google.gson.annotations.SerializedName

/**
 * DTO JSON từ GET /api/v1/theme.
 * [SerializedName] khớp key server — tránh lệch khi rename field Kotlin.
 */
data class ThemeResponse(
    @SerializedName("theme")
    val theme: String,

    @SerializedName("wallpaper")
    val wallpaper: String
)
