// File: app/src/main/java/com/example/appblocker/utils/TimeUtils.kt
package com.example.appblocker.utils

import com.example.appblocker.model.TimeRange
import java.util.*

object TimeUtils {

    fun isWithinRange(range: TimeRange): Boolean {
        val now = Calendar.getInstance()
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startMinutes = range.startHour * 60 + range.startMinute
        val endMinutes = range.endHour * 60 + range.endMinute

        return if (startMinutes <= endMinutes) {
            nowMinutes in startMinutes..endMinutes
        } else {
            // Crosses midnight
            nowMinutes >= startMinutes || nowMinutes <= endMinutes
        }
    }

    fun isInAnyRange(ranges: List<TimeRange>): Boolean {
        return ranges.any { isWithinRange(it) }
    }
}
