package com.example.appblocker.model

data class AppSchedule(
    val packageName: String,
    val timeRanges: List<TimeRange> = emptyList(),
    val daysOfWeek: Set<Int> = emptySet(), // 1=Sunday, 7=Saturday
    val isEnabled: Boolean = true
)