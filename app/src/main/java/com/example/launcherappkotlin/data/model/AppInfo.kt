package com.example.launcherappkotlin.data.model

import android.graphics.drawable.Drawable



data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable,
    val componentKey: String = "$packageName/$activityName",
    val hasCustomIcon: Boolean = false,
)