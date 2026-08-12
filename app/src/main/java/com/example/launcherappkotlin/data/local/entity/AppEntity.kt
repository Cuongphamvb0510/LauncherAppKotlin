package com.example.launcherappkotlin.data.local.entity
import androidx.room.PrimaryKey
import androidx.room.Entity

@Entity(tableName = "installed_apps")
data class AppEntity (
    @PrimaryKey val componentKey: String,
    val packageName: String,
    val activityName: String,
    val label: String
)
