package com.example.launcherappkotlin.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.example.launcherappkotlin.data.local.ImageFileHelper
import com.example.launcherappkotlin.data.local.LauncherPreferences
import com.example.launcherappkotlin.data.local.dao.AppDao
import com.example.launcherappkotlin.data.local.dao.IconOverrideDao
import com.example.launcherappkotlin.data.local.entity.AppEntity
import com.example.launcherappkotlin.data.local.entity.IconOverrideEntity
import com.example.launcherappkotlin.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class AppRepository(
    private val appDao: AppDao,
    private val iconOverrideDao: IconOverrideDao,
    private val packageManager: PackageManager,
    private val context: Context,
    private val preferences: LauncherPreferences
) {

    // ===== ĐỌC DANH SÁCH APP (có icon custom nếu có) =====
    fun observeApps(): Flow<List<AppInfo>> {
        return combine(
            appDao.observeAll(),
            iconOverrideDao.observeAll()
        ) { entities, overrides ->
            val overrideMap = overrides.associateBy { it.componentKey }
            entities.map { entity ->
                entity.toAppInfo(overrideMap[entity.componentKey])
            }
        }.flowOn(Dispatchers.IO)
    }

    // ===== WALLPAPER =====
    fun getWallpaperPath(): String? = preferences.getWallpaperPath()

    suspend fun setWallpaper(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "wallpaper.jpg")
        if (!ImageFileHelper.copyToInternal(context, uri, file)) return@withContext false
        preferences.setWallpaperPath(file.absolutePath)
        true
    }

    // ===== ICON CUSTOM =====
    suspend fun setCustomIcon(componentKey: String, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val safeKey = componentKey.replace("/", "_")
        val file = File(context.filesDir, "icons/$safeKey.jpg")
        if (!ImageFileHelper.copyToInternal(context, uri, file)) return@withContext false
        // Touch mtime so DiffUtil sees a new iconRevision even if path is unchanged
        file.setLastModified(System.currentTimeMillis())
        iconOverrideDao.upsert(
            IconOverrideEntity(
                componentKey = componentKey,
                iconPath = file.absolutePath
            )
        )
        true
    }

    suspend fun clearCustomIcon(componentKey: String) = withContext(Dispatchers.IO) {
        iconOverrideDao.delete(componentKey)
        val safeKey = componentKey.replace("/", "_")
        File(context.filesDir, "icons/$safeKey.jpg").delete()
    }

    // ===== SYNC APP (giữ nguyên logic cũ) =====
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

    // ===== CHUYỂN Entity → AppInfo (ưu tiên icon custom) =====
    private fun AppEntity.toAppInfo(override: IconOverrideEntity?): AppInfo {
        val iconFile = override?.iconPath?.let { File(it) }?.takeIf { it.exists() }
        val customIcon = iconFile?.absolutePath?.let {
            ImageFileHelper.loadDrawable(context, it)
        }
        val icon = customIcon ?: try {
            packageManager.getActivityIcon(
                ComponentName(packageName, activityName)
            )
        } catch (e: Exception) {
            packageManager.defaultActivityIcon
        }
        return AppInfo(
            label = label,
            packageName = packageName,
            activityName = activityName,
            icon = icon,
            componentKey = componentKey,
            hasCustomIcon = customIcon != null,
            iconRevision = iconFile?.lastModified() ?: 0L
        )
    }
}