package com.example.launcherappkotlin

import android.app.Application
import com.example.launcherappkotlin.data.api.NetworkModule
import com.example.launcherappkotlin.data.api.ThemeApi
import com.example.launcherappkotlin.data.local.AppDatabase
import com.example.launcherappkotlin.data.local.LauncherPreferences
import com.example.launcherappkotlin.data.remote.ThemeServer
import com.example.launcherappkotlin.data.repository.AppRepository
import com.example.launcherappkotlin.data.repository.ThemeRepository
import fi.iki.elonen.NanoHTTPD.SOCKET_READ_TIMEOUT

class LauncherApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val preferences by lazy { LauncherPreferences(this) }
    private var themeServer: ThemeServer? = null

    val themeApi: ThemeApi by lazy { NetworkModule.themeApi }

    val themeRepository by lazy {
        ThemeRepository(
            themeApi = themeApi,
            context = applicationContext,
            preferences = preferences
        )
    }

    val appRepository by lazy {
        AppRepository(
            appDao = database.appDao(),
            iconOverrideDao = database.iconOverrideDao(),
            packageManager = packageManager,
            context = applicationContext,
            preferences = preferences
        )
    }

    override fun onCreate() {
        super.onCreate()
        themeServer = ThemeServer()
        themeServer?.start(SOCKET_READ_TIMEOUT, false)
    }
}
