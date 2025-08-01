// File: app/src/main/java/com/example/appblocker/model/TimeRange.kt
package com.example.appblocker.model

data class TimeRange(
    var startHour: Int = 0,
    var startMinute: Int = 0,
    var endHour: Int = 0,
    var endMinute: Int = 0
) {
    fun isValid(): Boolean {
        return startHour != endHour || startMinute != endMinute
    }
    
    fun isTimeInRange(hour: Int, minute: Int): Boolean {
        val currentMinutes = hour * 60 + minute
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute
        
        return if (startMinutes <= endMinutes) {
            // Same day range
            currentMinutes in startMinutes..endMinutes
        } else {
            // Overnight range (e.g., 22:00 to 06:00)
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }
}
