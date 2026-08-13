package com.example.launcherappkotlin.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.launcherappkotlin.data.model.AppInfo
import com.example.launcherappkotlin.data.repository.AppRepository
import com.example.launcherappkotlin.data.repository.ThemeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Path alone is not enough: wallpaper is always saved as the same file. */
data class WallpaperUiState(val path: String?, val version: Long = 0L)

class LauncherViewModel(
    private val repository: AppRepository,
    private val themeRepository: ThemeRepository
) : ViewModel() {

    val apps: StateFlow<List<AppInfo>> = repository.observeApps()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
    private val _wallpaper = MutableStateFlow(WallpaperUiState(null))
    val wallpaper: StateFlow<WallpaperUiState> = _wallpaper.asStateFlow()

    private val _currentTheme = MutableStateFlow<String?>(null)
    val currentTheme: StateFlow<String?> = _currentTheme.asStateFlow()

    init {
        // Sync nền — UI đã hiện từ Room trước đó
        viewModelScope.launch {
            publishWallpaper(repository.getWallpaperPath())
            _currentTheme.value = themeRepository.getCurrentTheme()
            repository.syncInstalledApps()
        }
    }

    fun setWallpaper(uri: Uri) {
        viewModelScope.launch {
            if (repository.setWallpaper(uri)) {
                publishWallpaper(repository.getWallpaperPath())
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

    /** Đồng bộ lại list app (dùng khi quay về Home). */
    fun syncApps() {
        viewModelScope.launch {
            repository.syncInstalledApps()
        }
    }

    fun fetchThemeFromServer() {
        viewModelScope.launch {
            themeRepository.fetchAndApplyTheme()
                .onSuccess { theme ->
                    _currentTheme.value = theme.theme
                    // Force re-emit even when theme name / file path are unchanged
                    publishWallpaper(repository.getWallpaperPath())
                }
            // lỗi thì tạm bỏ qua (sau này có thể hiện Toast)
        }
    }

    /** Reset wallpaper, theme label, và mọi icon custom về mặc định. */
    fun resetThemeAndIcons() {
        viewModelScope.launch {
            repository.clearAllCustomIcons()
            repository.clearWallpaper()
            themeRepository.clearTheme()
            // Force emit nếu đang null (StateFlow bỏ qua giá trị trùng)
            if (_currentTheme.value == null) {
                _currentTheme.value = ""
            }
            _currentTheme.value = null
            publishWallpaper(null)
        }
    }

    private fun publishWallpaper(path: String?) {
        // StateFlow skips equal values; bump version so UI reloads the same path
        _wallpaper.value = WallpaperUiState(path, System.nanoTime())
    }
}

class LauncherViewModelFactory(
    private val repository: AppRepository,
    private val themeRepository: ThemeRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LauncherViewModel::class.java)) {
            return LauncherViewModel(repository, themeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}