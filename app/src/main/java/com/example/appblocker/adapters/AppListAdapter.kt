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
                
                val validRanges = schedule.timeRanges.filter { it.isValid() }
                if (validRanges.isNotEmpty()) {
                    val rangeCount = validRanges.size
                    val daysText = getDaysText(schedule.daysOfWeek)
                    val isCurrentlyActive = isScheduleActiveNow(schedule)
                    val statusText = if (isCurrentlyActive) "Active now" else "Inactive"
                    
                    if (rangeCount == 1) {
                        val timeRange = validRanges.first()
                        val startTime = String.format("%02d:%02d", timeRange.startHour, timeRange.startMinute)
                        val endTime = String.format("%02d:%02d", timeRange.endHour, timeRange.endMinute)
                        binding.scheduleText.text = "$startTime - $endTime • $daysText • $statusText"
                    } else {
                        binding.scheduleText.text = "$rangeCount time ranges • $daysText • $statusText"
                    }
                } else {
                    binding.scheduleText.text = "No valid time ranges"
                }
            } else {
                binding.scheduleText.visibility = View.GONE
            }
        } else {
            binding.scheduleButton.visibility = View.GONE
            binding.scheduleText.visibility = View.GONE
        }
    }
    
    private fun getDaysText(daysOfWeek: Set<Int>): String {
        return when {
            daysOfWeek.size == 7 -> "Daily"
            daysOfWeek.containsAll(listOf(2, 3, 4, 5, 6)) && daysOfWeek.size == 5 -> "Weekdays"
            daysOfWeek.containsAll(listOf(1, 7)) && daysOfWeek.size == 2 -> "Weekends"
            else -> "${daysOfWeek.size} days"
        }
    }
    
    private fun isScheduleActiveNow(schedule: com.example.appblocker.model.AppSchedule): Boolean {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        
        if (!schedule.daysOfWeek.contains(currentDay)) return false
        
        return schedule.timeRanges.any { range ->
            range.isValid() && range.isTimeInRange(currentHour, currentMinute)
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
