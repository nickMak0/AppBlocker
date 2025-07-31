package com.example.appblocker.utils

import android.content.Context
import com.example.appblocker.model.AppSchedule
import java.util.*

object AppScheduleChecker {
    
    fun hasAppSchedule(context: Context, packageName: String): Boolean {
        val schedule = AppScheduleStorage.getAppSchedule(context, packageName)
        return schedule != null && schedule.isEnabled && schedule.timeRanges.isNotEmpty()
    }
    
    fun isAppBlockedBySchedule(context: Context, packageName: String): Boolean {
        val schedule = AppScheduleStorage.getAppSchedule(context, packageName)
            ?: return false // No schedule means not blocked by time
        
        if (!schedule.isEnabled || schedule.timeRanges.isEmpty()) {
            return false
        }
        
        // Check if current day is in the schedule (if no days selected, apply to all days)
        if (schedule.daysOfWeek.isNotEmpty()) {
            val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            if (!schedule.daysOfWeek.contains(currentDay)) {
                return false
            }
        }
        
        // Check if current time is within any of the time ranges
        return schedule.timeRanges.any { timeRange ->
            TimeUtils.isWithinRange(timeRange)
        }
    }
}