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
}
