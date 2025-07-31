package com.example.appblocker.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.widget.TimePicker
import androidx.appcompat.app.AlertDialog
import com.example.appblocker.R
import com.example.appblocker.model.AppSchedule
import com.example.appblocker.model.TimeRange
import com.google.android.material.chip.Chip

object AppScheduleDialog {
    
    fun show(
        context: Context,
        packageName: String,
        appName: String,
        currentSchedule: AppSchedule?,
        onScheduleSet: (AppSchedule) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_app_schedule, null)
        
        val startTimePicker = view.findViewById<TimePicker>(R.id.startTimePicker)
        val endTimePicker = view.findViewById<TimePicker>(R.id.endTimePicker)
        
        // Set 24-hour format
        startTimePicker.setIs24HourView(true)
        endTimePicker.setIs24HourView(true)
        
        // Set default time if no current schedule
        if (currentSchedule == null) {
            startTimePicker.hour = 22 // 10 PM
            startTimePicker.minute = 0
            endTimePicker.hour = 6 // 6 AM
            endTimePicker.minute = 0
        }
        
        // Initialize with current schedule if exists
        currentSchedule?.let { schedule ->
            if (schedule.timeRanges.isNotEmpty()) {
                val timeRange = schedule.timeRanges.first()
                startTimePicker.hour = timeRange.startHour
                startTimePicker.minute = timeRange.startMinute
                endTimePicker.hour = timeRange.endHour
                endTimePicker.minute = timeRange.endMinute
            }
            
            // Set selected days
            schedule.daysOfWeek.forEach { dayOfWeek ->
                val chipId = when (dayOfWeek) {
                    1 -> R.id.chipSunday
                    2 -> R.id.chipMonday
                    3 -> R.id.chipTuesday
                    4 -> R.id.chipWednesday
                    5 -> R.id.chipThursday
                    6 -> R.id.chipFriday
                    7 -> R.id.chipSaturday
                    else -> null
                }
                chipId?.let { view.findViewById<Chip>(it).isChecked = true }
            }
        }
        
        // Set up dialog
        AlertDialog.Builder(context)
            .setTitle("Schedule for $appName")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val selectedDays = mutableSetOf<Int>()
                
                if (view.findViewById<Chip>(R.id.chipSunday).isChecked) selectedDays.add(1)
                if (view.findViewById<Chip>(R.id.chipMonday).isChecked) selectedDays.add(2)
                if (view.findViewById<Chip>(R.id.chipTuesday).isChecked) selectedDays.add(3)
                if (view.findViewById<Chip>(R.id.chipWednesday).isChecked) selectedDays.add(4)
                if (view.findViewById<Chip>(R.id.chipThursday).isChecked) selectedDays.add(5)
                if (view.findViewById<Chip>(R.id.chipFriday).isChecked) selectedDays.add(6)
                if (view.findViewById<Chip>(R.id.chipSaturday).isChecked) selectedDays.add(7)
                
                val timeRange = TimeRange(
                    startHour = startTimePicker.hour,
                    startMinute = startTimePicker.minute,
                    endHour = endTimePicker.hour,
                    endMinute = endTimePicker.minute
                )
                
                // If no days selected, apply to all days
                val finalDays = if (selectedDays.isEmpty()) {
                    setOf(1, 2, 3, 4, 5, 6, 7) // All days
                } else {
                    selectedDays
                }
                
                val schedule = AppSchedule(
                    packageName = packageName,
                    timeRanges = listOf(timeRange),
                    daysOfWeek = finalDays,
                    isEnabled = true
                )
                
                onScheduleSet(schedule)
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Remove Schedule") { _, _ ->
                val emptySchedule = AppSchedule(
                    packageName = packageName,
                    timeRanges = emptyList(),
                    daysOfWeek = emptySet(),
                    isEnabled = false
                )
                onScheduleSet(emptySchedule)
            }
            .show()
    }
}