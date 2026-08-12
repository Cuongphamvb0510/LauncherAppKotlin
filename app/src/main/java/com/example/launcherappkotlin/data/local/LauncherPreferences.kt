package com.example.launcherappkotlin.data.local

import android.content.Context

class LauncherPreferences(context: Context) {

    // Lấy SharedPreferences tên "launcher_prefs"
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Đọc path wallpaper đã lưu (null = chưa đặt)
    fun getWallpaperPath(): String? {
        return prefs.getString(KEY_WALLPAPER, null)

    }
    // Lưu path wallpaper
    fun setWallpaperPath(path: String?) {
        prefs.edit()
            .putString(KEY_WALLPAPER, path)
            .apply()
    }

    fun getThemeName(): String? = prefs.getString(KEY_THEME, null)

    fun setThemeName(name: String?) {
        prefs.edit().putString(KEY_THEME, name).apply()
    }


    companion object {
        private const val PREFS_NAME = "launcher_prefs"
        private const val KEY_WALLPAPER = "wallpaper_path"
        private const val KEY_THEME = "theme_name"

    }
}