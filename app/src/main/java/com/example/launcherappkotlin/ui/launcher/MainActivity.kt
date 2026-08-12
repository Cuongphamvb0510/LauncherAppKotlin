package com.example.launcherappkotlin.ui.launcher

import android.graphics.BitmapFactory
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.example.launcherappkotlin.R

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: AppGridAdapter
    private var pendingIconApp: AppInfo? = null
    private val viewModel: LauncherViewModel by viewModels {
        val app = application as LauncherApp
        LauncherViewModelFactory(app.appRepository)
    }

    private val pickWallpaper = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.setWallpaper(it) }
    }

    private val pickIcon = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        val app = pendingIconApp
        pendingIconApp = null
        if (uri != null && app != null) {
            viewModel.setCustomIcon(app.componentKey, uri)
        }
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

        adapter = AppGridAdapter(
            onClick = { openApp(it) },
            onLongClick = { showIconOptions(it) }
        )

        binding.rvApps.layoutManager = GridLayoutManager(this, 4)
        binding.rvApps.adapter = adapter

        binding.fabWallpaper.setOnClickListener {
            pickWallpaper.launch("image/*")
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.apps.collect { apps ->
                        adapter.submitList(apps)
                    }
                }
                launch {
                    viewModel.wallpaperPath.collect { path ->
                        if (path == null) {
                            binding.ivWallpaper.setImageDrawable(null)
                        } else {
                            val bitmap = BitmapFactory.decodeFile(path)
                            binding.ivWallpaper.setImageBitmap(bitmap)
                        }
                    }
                }
            }
        }

    }

    private fun showIconOptions(app: AppInfo) {
        val options = mutableListOf(getString(R.string.change_icon))
        if (app.hasCustomIcon) {
            options.add(getString(R.string.reset_icon))
        }
        options.add(getString(R.string.cancel))

        AlertDialog.Builder(this)
            .setTitle(app.label)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    getString(R.string.change_icon) -> {
                        pendingIconApp = app
                        pickIcon.launch("image/*")
                    }
                    getString(R.string.reset_icon) -> {
                        viewModel.clearCustomIcon(app.componentKey)
                    }
                }
            }
            .show()
    }

    private fun openApp(app: AppInfo) {
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) startActivity(launchIntent)
    }

}