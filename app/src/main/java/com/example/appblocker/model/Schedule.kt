// File: app/src/main/java/com/example/appblocker/model/Schedule.kt
package com.example.appblocker.model

data class Schedule(
    val id: Int,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeek: Set<Int> // 1=Sunday, 7=Saturday
)

