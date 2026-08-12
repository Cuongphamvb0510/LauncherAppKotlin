package com.example.launcherappkotlin.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.launcherappkotlin.LauncherApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Chỉ xử lý khi package thay đổi
        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                val app = context.applicationContext as LauncherApp
                // Broadcast chạy trên main thread → dùng coroutine
                CoroutineScope(Dispatchers.IO).launch {
                    app.appRepository.syncInstalledApps()
                }
            }
        }
    }
}