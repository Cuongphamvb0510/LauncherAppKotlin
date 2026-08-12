package com.example.launcherappkotlin.data.repository

import android.content.Context
import com.example.launcherappkotlin.data.api.ThemeApi
import com.example.launcherappkotlin.data.local.ImageFileHelper
import com.example.launcherappkotlin.data.local.LauncherPreferences
import com.example.launcherappkotlin.data.model.ThemeResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ThemeRepository(
    private val themeApi: ThemeApi,
    private val context: Context,
    private val preferences: LauncherPreferences
) {
    fun getCurrentTheme(): String? = preferences.getThemeName()

    suspend fun fetchAndApplyTheme(): Result<ThemeResponse> = withContext(Dispatchers.IO) {
        try {
            // 1. Gọi API lấy theme info
            val theme = themeApi.getTheme()

            // 2. Lưu tên theme
            preferences.setThemeName(theme.theme)

            // 3. Tải wallpaper từ URL server trả về
            val wallpaperFile = File(context.filesDir, "wallpaper.jpg")
            ImageFileHelper.downloadFromUrl(theme.wallpaper, wallpaperFile)

            // 4. Lưu path wallpaper (dùng lại logic Bước 5 cũ)
            preferences.setWallpaperPath(wallpaperFile.absolutePath)

            Result.success(theme)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}