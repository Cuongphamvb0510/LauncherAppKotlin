package com.example.launcherappkotlin.data.repository

import android.content.Intent
import android.content.pm.PackageManager
import com.example.launcherappkotlin.data.local.dao.AppDao
import com.example.launcherappkotlin.data.local.entity.AppEntity
import com.example.launcherappkotlin.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AppRepository(
    private val appDao: AppDao,
    private val packageManager: PackageManager
) {
    fun observeApps(): Flow<List<AppInfo>> {
        return appDao.observeAll()
            .map { entities -> entities.map { it.toAppInfo() } }
            .flowOn(Dispatchers.IO)
    }

    suspend fun syncInstalledApps() = withContext(Dispatchers.IO) {
        val fromSystem = queryLauncherApps()
        appDao.replaceAll(fromSystem)
    }

    private fun queryLauncherApps(): List<AppEntity> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return packageManager.queryIntentActivities(intent, 0)
            .map { info ->
                val packageName = info.activityInfo.packageName
                val activityName = info.activityInfo.name
                AppEntity(
                    componentKey = "$packageName/$activityName",
                    packageName = packageName,
                    activityName = activityName,
                    label = info.loadLabel(packageManager).toString()
                )
            }
    }

    private fun AppEntity.toAppInfo(): AppInfo {
        val icon = try {
            packageManager.getActivityIcon(
                android.content.ComponentName(packageName, activityName)
            )
        } catch (_: Exception) {
            packageManager.defaultActivityIcon
        }
        return AppInfo(
            label = label,
            packageName = packageName,
            activityName = activityName,
            icon = icon
        )
    }
}