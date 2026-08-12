package com.example.launcherappkotlin.ui.launcher

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.launcherappkotlin.data.model.AppInfo
import com.example.launcherappkotlin.databinding.ItemAppBinding

class AppGridAdapter(
    private val onClick: (AppInfo) -> Unit,
    private val onLongClick: (AppInfo) -> Unit
) : ListAdapter<AppInfo, AppGridAdapter.AppViewHolder>(DiffCallback) {

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
        val app = getItem(position)
        holder.binding.ivIcon.setImageDrawable(app.icon)
        holder.binding.tvLabel.text = app.label
        holder.binding.root.setOnClickListener { onClick(app) }
        holder.binding.root.setOnLongClickListener {
            onLongClick(app)
            true
        }
        labelColor?.let { holder.binding.tvLabel.setTextColor(it) }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(old: AppInfo, new: AppInfo) =
            old.packageName == new.packageName && old.activityName == new.activityName
        override fun areContentsTheSame(old: AppInfo, new: AppInfo) =
            old.label == new.label && old.hasCustomIcon == new.hasCustomIcon
    }


    private var labelColor: Int? = null

    fun setLabelColor(color: Int) {
        labelColor = color
        notifyDataSetChanged()
    }

}