// File: app/src/main/java/com/example/appblocker/AppListAdapter.kt
package com.example.appblocker

import android.content.Context
import android.content.pm.ApplicationInfo
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appblocker.databinding.ItemAppBinding

class AppListAdapter(
    private val context: Context,
    private val apps: List<ApplicationInfo>,
    private val blockedApps: MutableSet<String>,
    private val onBlockToggle: (String, Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    inner class AppViewHolder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(context), parent, false)
        return AppViewHolder(binding)
    }

    override fun getItemCount(): Int = apps.size

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val appInfo = apps[position]
        val packageName = appInfo.packageName
        val appName = appInfo.loadLabel(context.packageManager).toString()
        val appIcon = appInfo.loadIcon(context.packageManager)

        with(holder.binding) {
            appNameText.text = appName
            appIconImage.setImageDrawable(appIcon)

            // 🔁 Avoid multiple listener calls
            blockSwitch.setOnCheckedChangeListener(null)
            blockSwitch.isChecked = blockedApps.contains(packageName)

            blockSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) blockedApps.add(packageName)
                else blockedApps.remove(packageName)

                onBlockToggle(packageName, isChecked)

                // ✅ Force state update (optional but helpful)
                notifyItemChanged(position)
            }
        }
    }
}
