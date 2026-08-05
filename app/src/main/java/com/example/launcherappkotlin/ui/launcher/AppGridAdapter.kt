package com.example.launcherappkotlin.ui.launcher

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.launcherappkotlin.data.model.AppInfo
import com.example.launcherappkotlin.databinding.ItemAppBinding

class AppGridAdapter(
    private val apps: List<AppInfo>,
    private val onClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppGridAdapter.AppViewHolder>() {

    class AppViewHolder(
        val binding: ItemAppBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.binding.ivIcon.setImageDrawable(app.icon)
        holder.binding.tvLabel.text = app.label
        holder.binding.root.setOnClickListener { onClick(app) }
    }

    override fun getItemCount(): Int = apps.size
}