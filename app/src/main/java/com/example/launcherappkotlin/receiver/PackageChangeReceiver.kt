package com.example.launcherappkotlin.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.launcherappkotlin.LauncherApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Backup cho [PackageMonitor]: khi Manifest nhận được PACKAGE_*.
 * Gỡ app → [AppRepository.removePackage] (xóa Room ngay), không chỉ full sync.
 */
class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val packageName = intent.data?.schemeSpecificPart ?: return
        val app = context.applicationContext as? LauncherApp ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    Intent.ACTION_PACKAGE_REMOVED -> {
                        if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                            app.appRepository.removePackage(packageName)
                        }
                    }
                    Intent.ACTION_PACKAGE_ADDED,
                    Intent.ACTION_PACKAGE_CHANGED,
                    Intent.ACTION_PACKAGE_REPLACED -> {
                        app.appRepository.syncInstalledApps()
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
