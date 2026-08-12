package com.example.launcherappkotlin.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.launcherappkotlin.data.model.AppInfo
import com.example.launcherappkotlin.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LauncherViewModel(
    private val repository: AppRepository
) : ViewModel() {

    val apps: StateFlow<List<AppInfo>> = repository.observeApps()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
    private val _wallpaperPath = MutableStateFlow<String?>(null)
    val wallpaperPath: StateFlow<String?> = _wallpaperPath.asStateFlow()

    init {
        // Sync nền — UI đã hiện từ Room trước đó
        viewModelScope.launch {
            _wallpaperPath.value = repository.getWallpaperPath()
            repository.syncInstalledApps()
        }
    }

    fun setWallpaper(uri: Uri) {
        viewModelScope.launch {
            if (repository.setWallpaper(uri)) {
                _wallpaperPath.value = repository.getWallpaperPath()
            }
        }
    }

    fun setCustomIcon(componentKey: String, uri: Uri) {
        viewModelScope.launch {
            repository.setCustomIcon(componentKey, uri)
        }
    }

    fun clearCustomIcon(componentKey: String) {
        viewModelScope.launch {
            repository.clearCustomIcon(componentKey)
        }
    }
}

class LauncherViewModelFactory(
    private val repository: AppRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LauncherViewModel::class.java)) {
            return LauncherViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}