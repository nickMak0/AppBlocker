// File: app/src/main/java/com/example/appblocker/adapters/AppListAdapter.kt
package com.example.appblocker.adapters

import android.content.Context
import android.content.pm.ApplicationInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appblocker.databinding.ItemAppToggleBinding
import com.example.appblocker.utils.AppScheduleStorage
import java.text.SimpleDateFormat
import java.util.*

class AppListAdapter(
    private val context: Context,
    private var apps: List<ApplicationInfo>,
    private val blockedApps: MutableSet<String>,
    private val onToggleChanged: (String, Boolean) -> Unit,
    private val onScheduleClicked: (String, String) -> Unit
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
        val isBlocked = blockedApps.contains(pkg)

        holder.binding.apply {
            appNameText.text = name
            appIcon.setImageDrawable(icon)

            // Prevent toggling triggering the listener unnecessarily
            blockToggle.setOnCheckedChangeListener(null)
            blockToggle.isChecked = isBlocked

            blockToggle.setOnCheckedChangeListener { _, isChecked ->
                onToggleChanged(pkg, isChecked)
                updateScheduleVisibility(this, pkg, isChecked)
            }

            // Update schedule visibility and text
            updateScheduleVisibility(this, pkg, isBlocked)

            // Set schedule button click listener
            scheduleButton.setOnClickListener {
                onScheduleClicked(pkg, name)
            }
        }
    }

    private fun updateScheduleVisibility(binding: ItemAppToggleBinding, packageName: String, isBlocked: Boolean) {
        if (isBlocked) {
            binding.scheduleButton.visibility = View.VISIBLE
            
            val schedule = AppScheduleStorage.getAppSchedule(context, packageName)
            if (schedule != null && schedule.isEnabled && schedule.timeRanges.isNotEmpty()) {
                binding.scheduleText.visibility = View.VISIBLE
                val timeRange = schedule.timeRanges.first()
                val startTime = String.format("%02d:%02d", timeRange.startHour, timeRange.startMinute)
                val endTime = String.format("%02d:%02d", timeRange.endHour, timeRange.endMinute)
                val duration = calculateDuration(timeRange.startHour, timeRange.startMinute, timeRange.endHour, timeRange.endMinute)
                val daysText = if (schedule.daysOfWeek.size == 7) "Daily" else "${schedule.daysOfWeek.size} days"
                binding.scheduleText.text = "$startTime - $endTime ($duration, $daysText)"
            } else {
                binding.scheduleText.visibility = View.GONE
            }
        } else {
            binding.scheduleButton.visibility = View.GONE
            binding.scheduleText.visibility = View.GONE
        }
    }

    fun updateApps(newList: List<ApplicationInfo>) {
        apps = newList
        notifyDataSetChanged()
    }
    
    fun refreshScheduleInfo() {
        notifyDataSetChanged()
    }
    
    private fun calculateDuration(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int): String {
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute
        
        val durationMinutes = if (endMinutes > startMinutes) {
            endMinutes - startMinutes
        } else {
            // Crosses midnight
            (24 * 60) - startMinutes + endMinutes
        }
        
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60
        
        return when {
            hours == 0 -> "${minutes}m"
            minutes == 0 -> "${hours}h"
            else -> "${hours}h ${minutes}m"
        }
    }
}
