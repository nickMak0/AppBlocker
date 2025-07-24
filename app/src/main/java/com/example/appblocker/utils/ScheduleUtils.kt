// File: app/src/main/java/com/example/appblocker/utils/ScheduleUtils.kt
package com.example.appblocker.utils

import android.content.Context
import com.example.appblocker.model.TimeRange
import java.util.*

object ScheduleUtils {

    fun isBlockingActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)

        val selectedDays = prefs.getStringSet("schedule_days_of_week", emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: return false

        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        if (!selectedDays.contains(today)) return false

        val json = prefs.getString("time_ranges", null) ?: return false
        val timeRanges = TimeRangeStorage.deserialize(json)

        return timeRanges.any { TimeUtils.isWithinRange(it) }
    }
}
