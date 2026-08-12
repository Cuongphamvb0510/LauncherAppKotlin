package com.example.launcherappkotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.launcherappkotlin.data.model.AppInfo
import com.example.launcherappkotlin.data.repository.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    init {
        // Sync nền — UI đã hiện từ Room trước đó
        viewModelScope.launch {
            repository.syncInstalledApps()
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