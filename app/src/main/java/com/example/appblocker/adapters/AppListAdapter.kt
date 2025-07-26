// File: app/src/main/java/com/example/appblocker/adapters/AppListAdapter.kt
package com.example.appblocker.adapters

import android.content.Context
import android.content.pm.ApplicationInfo
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appblocker.databinding.ItemAppToggleBinding

class AppListAdapter(
    private val context: Context,
    private var apps: List<ApplicationInfo>,
    private val blockedApps: MutableSet<String>,
    private val onToggleChanged: (String, Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    inner class AppViewHolder(val binding: ItemAppToggleBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppToggleBinding.inflate(LayoutInflater.from(context), parent, false)
        return AppViewHolder(binding)
    }

    override fun getItemCount(): Int = apps.size

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        val pkg = app.packageName
        val name = app.loadLabel(context.packageManager).toString()
        val icon = app.loadIcon(context.packageManager)

        holder.binding.apply {
            appNameText.text = name
            appIcon.setImageDrawable(icon)

            // Prevent toggling triggering the listener unnecessarily
            blockToggle.setOnCheckedChangeListener(null)
            blockToggle.isChecked = blockedApps.contains(pkg)

            blockToggle.setOnCheckedChangeListener { _, isChecked ->
                onToggleChanged(pkg, isChecked)
            }
        }
    }

    fun updateApps(newList: List<ApplicationInfo>) {
        apps = newList
        notifyDataSetChanged()
    }
}
