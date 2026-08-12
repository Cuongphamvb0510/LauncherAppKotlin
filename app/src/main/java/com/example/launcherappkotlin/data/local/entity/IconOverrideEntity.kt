package com.example.launcherappkotlin.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "icon_overrides")
data class IconOverrideEntity(
    @PrimaryKey val componentKey: String,  // "com.app/.MainActivity"
    val iconPath: String                   // "/data/.../icons/com.app_MainActivity.jpg"
)
