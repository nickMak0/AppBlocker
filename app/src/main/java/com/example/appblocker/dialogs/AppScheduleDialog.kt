package com.example.appblocker.dialogs

import android.app.TimePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appblocker.R
import com.example.appblocker.adapters.TimeRangeAdapter
import com.example.appblocker.model.AppSchedule
import com.example.appblocker.model.TimeRange
import com.google.android.material.chip.Chip
import java.util.*

object AppScheduleDialog {
    
    fun show(
        context: Context,
        packageName: String,
        appName: String,
        currentSchedule: AppSchedule?,
        onScheduleSet: (AppSchedule) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_app_schedule_enhanced, null)
        
        val dialogTitle = view.findViewById<android.widget.TextView>(R.id.dialogTitle)
        val scheduleStatusText = view.findViewById<android.widget.TextView>(R.id.scheduleStatusText)
        val timeRangesRecyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.timeRangesRecyclerView)
        val emptyTimeRangesText = view.findViewById<android.widget.TextView>(R.id.emptyTimeRangesText)
        val addTimeRangeButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.addTimeRangeButton)
        val weekdaysButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.weekdaysButton)
        val weekendsButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.weekendsButton)
        val saveButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.saveButton)
        val cancelButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancelButton)
        val removeScheduleButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.removeScheduleButton)
        
        dialogTitle.text = "Schedule for $appName"
        
        val timeRanges = mutableListOf<TimeRange>()
        val selectedDays = mutableSetOf<Int>()
        lateinit var adapter: TimeRangeAdapter
        
        // Initialize with current schedule if exists
        currentSchedule?.let { schedule ->
            timeRanges.addAll(schedule.timeRanges)
            selectedDays.addAll(schedule.daysOfWeek)
            
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
        
        fun updateSelectedDays() {
            selectedDays.clear()
            if (view.findViewById<Chip>(R.id.chipSunday).isChecked) selectedDays.add(1)
            if (view.findViewById<Chip>(R.id.chipMonday).isChecked) selectedDays.add(2)
            if (view.findViewById<Chip>(R.id.chipTuesday).isChecked) selectedDays.add(3)
            if (view.findViewById<Chip>(R.id.chipWednesday).isChecked) selectedDays.add(4)
            if (view.findViewById<Chip>(R.id.chipThursday).isChecked) selectedDays.add(5)
            if (view.findViewById<Chip>(R.id.chipFriday).isChecked) selectedDays.add(6)
            if (view.findViewById<Chip>(R.id.chipSaturday).isChecked) selectedDays.add(7)
        }
        
        fun isScheduleActiveNow(): Boolean {
            val calendar = Calendar.getInstance()
            val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            
            if (!selectedDays.contains(currentDay)) return false
            
            return timeRanges.any { range ->
                range.isValid() && range.isTimeInRange(currentHour, currentMinute)
            }
        }
        
        fun updateScheduleStatus() {
            val hasTimeRanges = timeRanges.isNotEmpty() && timeRanges.any { it.isValid() }
            val hasDays = selectedDays.isNotEmpty()
            
            when {
                !hasDays -> scheduleStatusText.text = "No days selected"
                !hasTimeRanges -> scheduleStatusText.text = "No time ranges set"
                else -> {
                    val isCurrentlyActive = isScheduleActiveNow()
                    scheduleStatusText.text = if (isCurrentlyActive) "Schedule active now" else "Schedule inactive now"
                }
            }
        }
        
        fun updateUI() {
            if (timeRanges.isEmpty()) {
                emptyTimeRangesText.visibility = android.view.View.VISIBLE
                timeRangesRecyclerView.visibility = android.view.View.GONE
            } else {
                emptyTimeRangesText.visibility = android.view.View.GONE
                timeRangesRecyclerView.visibility = android.view.View.VISIBLE
            }
            adapter.notifyDataSetChanged()
            updateSelectedDays()
            updateScheduleStatus()
        }
        
        // Set up RecyclerView
        adapter = TimeRangeAdapter(
            timeRanges,
            onRemove = { position ->
                timeRanges.removeAt(position)
                updateUI()
            },
            onTimeClick = { position, isStartTime ->
                val range = timeRanges[position]
                val hour = if (isStartTime) range.startHour else range.endHour
                val minute = if (isStartTime) range.startMinute else range.endMinute
                
                TimePickerDialog(context, { _, selectedHour, selectedMinute ->
                    if (isStartTime) {
                        timeRanges[position] = range.copy(startHour = selectedHour, startMinute = selectedMinute)
                    } else {
                        timeRanges[position] = range.copy(endHour = selectedHour, endMinute = selectedMinute)
                    }
                    updateUI()
                }, hour, minute, true).show()
            }
        )
        
        timeRangesRecyclerView.layoutManager = LinearLayoutManager(context)
        timeRangesRecyclerView.adapter = adapter
        
        updateUI()
        
        // Day chip listeners
        listOf(
            R.id.chipSunday, R.id.chipMonday, R.id.chipTuesday, R.id.chipWednesday,
            R.id.chipThursday, R.id.chipFriday, R.id.chipSaturday
        ).forEach { chipId ->
            view.findViewById<Chip>(chipId).setOnCheckedChangeListener { _, _ ->
                updateUI()
            }
        }
        
        // Quick day selection buttons
        weekdaysButton.setOnClickListener {
            val weekdays = listOf(R.id.chipMonday, R.id.chipTuesday, R.id.chipWednesday, R.id.chipThursday, R.id.chipFriday)
            val allWeekdaysSelected = weekdays.all { view.findViewById<Chip>(it).isChecked }
            val newState = !allWeekdaysSelected
            
            weekdays.forEach { chipId ->
                view.findViewById<Chip>(chipId).isChecked = newState
            }
            updateUI()
        }
        
        weekendsButton.setOnClickListener {
            val weekends = listOf(R.id.chipSaturday, R.id.chipSunday)
            val allWeekendsSelected = weekends.all { view.findViewById<Chip>(it).isChecked }
            val newState = !allWeekendsSelected
            
            weekends.forEach { chipId ->
                view.findViewById<Chip>(chipId).isChecked = newState
            }
            updateUI()
        }
        
        // Add time range button
        addTimeRangeButton.setOnClickListener {
            timeRanges.add(TimeRange())
            updateUI()
        }
        
        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .create()
        
        saveButton.setOnClickListener {
            updateSelectedDays()
            
            if (selectedDays.isEmpty()) {
                Toast.makeText(context, "Please select at least one day", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val validRanges = timeRanges.filter { it.isValid() }
            if (validRanges.isEmpty()) {
                Toast.makeText(context, "Please add at least one valid time range", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val schedule = AppSchedule(
                packageName = packageName,
                timeRanges = validRanges,
                daysOfWeek = selectedDays,
                isEnabled = true
            )
            
            onScheduleSet(schedule)
            Toast.makeText(context, "Schedule saved for $appName", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        removeScheduleButton.setOnClickListener {
            val emptySchedule = AppSchedule(
                packageName = packageName,
                timeRanges = emptyList(),
                daysOfWeek = emptySet(),
                isEnabled = false
            )
            onScheduleSet(emptySchedule)
            Toast.makeText(context, "Schedule removed for $appName", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        
        dialog.show()
    }
}