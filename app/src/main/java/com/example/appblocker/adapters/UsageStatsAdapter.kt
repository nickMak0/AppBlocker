// File: app/src/main/java/com/example/appblocker/adapters/UsageStatsAdapter.kt
package com.example.appblocker.adapters

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.appblocker.databinding.ItemUsageStatBinding
import com.example.appblocker.model.UsageStatItem
import java.util.concurrent.TimeUnit

class UsageStatsAdapter(
    private var usageStats: List<UsageStatItem>,
    private val pm: PackageManager
) : RecyclerView.Adapter<UsageStatsAdapter.UsageViewHolder>() {

    inner class UsageViewHolder(val binding: ItemUsageStatBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsageViewHolder {
        val binding = ItemUsageStatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UsageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UsageViewHolder, position: Int) {
        val item = usageStats[position]
        val icon = try {
            pm.getApplicationIcon(item.packageName)
        } catch (e: Exception) {
            ContextCompat.getDrawable(holder.itemView.context, android.R.drawable.sym_def_app_icon)
        }

        holder.binding.apply {
            appName.text = item.appName
            appIcon.setImageDrawable(icon)
            usageTime.text = formatTime(item.minutesUsed)
        }
    }

    override fun getItemCount(): Int = usageStats.size

    fun updateData(newStats: List<UsageStatItem>) {
        usageStats = newStats
        notifyDataSetChanged()
    }

    private fun formatTime(mins: Long): String {
        val h = TimeUnit.MINUTES.toHours(mins)
        val rem = mins % 60
        return if (h > 0) "${h}h ${rem}m" else "${rem}m"
    }
}
