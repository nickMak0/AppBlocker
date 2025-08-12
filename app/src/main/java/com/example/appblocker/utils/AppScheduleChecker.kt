package com.example.appblocker.utils

import android.content.Context
import com.example.appblocker.model.AppSchedule
import java.util.*

object AppScheduleChecker {
    
    /**
     * Check if an app should be blocked based on its individual schedule
     */
    fun isAppBlockedBySchedule(context: Context, packageName: String): Boolean {
        val schedule = AppScheduleStorage.getAppSchedule(context, packageName)
        return schedule?.let { isScheduleActive(it) } ?: false
    }
    
    /**
     * Check if a schedule is currently active
     */
    fun isScheduleActive(schedule: AppSchedule): Boolean {
        if (!schedule.isEnabled || schedule.timeRanges.isEmpty()) return false
        
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        
        // Check if today is in the schedule
        if (!schedule.daysOfWeek.contains(currentDay)) return false
        
        // Check if current time is in any of the time ranges
        return schedule.timeRanges.any { range ->
            range.isValid() && range.isTimeInRange(currentHour, currentMinute)
        }
    }
    
    /**
     * Get next schedule activation time for an app
     */
    fun getNextActivationTime(schedule: AppSchedule): Calendar? {
        if (!schedule.isEnabled || schedule.timeRanges.isEmpty()) return null
        
        val now = Calendar.getInstance()
        val nextActivation = Calendar.getInstance()
        
        // Check today first
        for (range in schedule.timeRanges.filter { it.isValid() }) {
            nextActivation.set(Calendar.HOUR_OF_DAY, range.startHour)
            nextActivation.set(Calendar.MINUTE, range.startMinute)
            nextActivation.set(Calendar.SECOND, 0)
            
            if (nextActivation.after(now) && schedule.daysOfWeek.contains(now.get(Calendar.DAY_OF_WEEK))) {
                return nextActivation
            }
        }
        
        // Check next 7 days
        for (i in 1..7) {
            nextActivation.add(Calendar.DAY_OF_YEAR, 1)
            val dayOfWeek = nextActivation.get(Calendar.DAY_OF_WEEK)
            
            if (schedule.daysOfWeek.contains(dayOfWeek)) {
                val firstRange = schedule.timeRanges.filter { it.isValid() }.minByOrNull { it.startHour * 60 + it.startMinute }
                firstRange?.let { range ->
                    nextActivation.set(Calendar.HOUR_OF_DAY, range.startHour)
                    nextActivation.set(Calendar.MINUTE, range.startMinute)
                    nextActivation.set(Calendar.SECOND, 0)
                    return nextActivation
                }
            }
        }
        
        return null
    }
    
    /**
     * Get a human-readable description of when the schedule will be active next
     */
    fun getNextActivationDescription(schedule: AppSchedule): String {
        val nextTime = getNextActivationTime(schedule) ?: return "No upcoming schedule"
        
        val now = Calendar.getInstance()
        val diffInMillis = nextTime.timeInMillis - now.timeInMillis
        val diffInMinutes = diffInMillis / (1000 * 60)
        
        return when {
            diffInMinutes < 60 -> "Active in ${diffInMinutes}m"
            diffInMinutes < 24 * 60 -> {
                val hours = diffInMinutes / 60
                val minutes = diffInMinutes % 60
                if (minutes == 0L) "Active in ${hours}h" else "Active in ${hours}h ${minutes}m"
            }
            else -> {
                val days = diffInMinutes / (24 * 60)
                "Active in ${days}d"
            }
        }
    }
}