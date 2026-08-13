package com.example.launcherappkotlin.receiver

import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import com.example.launcherappkotlin.LauncherApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Cách chuẩn của Home Launcher để observe cài/gỡ app realtime.
 * Tin cậy hơn Broadcast PACKAGE_* (hay bị OEM / Manifest hạn chế).
 */
class PackageMonitor(private val app: LauncherApp) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val launcherApps = app.getSystemService(LauncherApps::class.java)

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            scope.launch { app.appRepository.removePackage(packageName) }
        }

        override fun onPackageAdded(packageName: String, user: UserHandle) {
            scope.launch { app.appRepository.syncInstalledApps() }
        }

        override fun onPackageChanged(packageName: String, user: UserHandle) {
            scope.launch { app.appRepository.syncInstalledApps() }
        }

        override fun onPackagesAvailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean
        ) {
            scope.launch { app.appRepository.syncInstalledApps() }
        }

        override fun onPackagesUnavailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean
        ) {
            if (replacing) return
            scope.launch {
                packageNames.forEach { app.appRepository.removePackage(it) }
            }
        }
    }

    fun start() {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
    }

    fun stop() {
        launcherApps.unregisterCallback(callback)
    }
}
