package com.example.launcherappkotlin

import android.app.Application
import com.example.launcherappkotlin.data.local.AppDatabase
import com.example.launcherappkotlin.data.local.LauncherPreferences
import com.example.launcherappkotlin.data.repository.AppRepository

class LauncherApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val preferences by lazy { LauncherPreferences(this) }

    val appRepository by lazy {
        AppRepository(
            appDao = database.appDao(),
            iconOverrideDao = database.iconOverrideDao(),
            packageManager = packageManager,
            context = applicationContext,
            preferences = preferences
        )
    }
}