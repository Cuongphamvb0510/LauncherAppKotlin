package com.example.launcherappkotlin.ui.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.launcherappkotlin.LauncherApp
import com.example.launcherappkotlin.data.model.AppInfo
import com.example.launcherappkotlin.databinding.ActivityMainBinding
import com.example.launcherappkotlin.viewmodel.LauncherViewModel
import com.example.launcherappkotlin.viewmodel.LauncherViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: AppGridAdapter
    private val viewModel: LauncherViewModel by viewModels {
        val app = application as LauncherApp
        LauncherViewModelFactory(app.appRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        onBackPressedDispatcher.addCallback(this) {
            // không thoát launcher
        }

        adapter = AppGridAdapter { openApp(it) }
        binding.rvApps.layoutManager = GridLayoutManager(this, 4)
        binding.rvApps.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.apps.collect { apps ->
                    adapter.submitList(apps)
                }
            }
        }
    }


    private fun openApp(app: AppInfo) {
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) startActivity(launchIntent)
    }
}