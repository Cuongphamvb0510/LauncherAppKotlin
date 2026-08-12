package com.example.launcherappkotlin

import android.app.Application
import com.example.launcherappkotlin.data.local.AppDatabase
import com.example.launcherappkotlin.data.repository.AppRepository

class LauncherApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val appRepository by lazy {
        AppRepository(database.appDao(), packageManager)
    }
}