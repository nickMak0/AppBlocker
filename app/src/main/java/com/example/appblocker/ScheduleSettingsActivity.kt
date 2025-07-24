package com.example.appblocker

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appblocker.adapters.TimeRangeAdapter
import com.example.appblocker.model.TimeRange
import com.example.appblocker.utils.PinUtils
import com.example.appblocker.utils.TimeRangeStorage
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class ScheduleSettingsActivity : AppCompatActivity() {

    private lateinit var backButton: MaterialButton
    private lateinit var cancelButton: MaterialButton
    private lateinit var statusChip: Chip
    private lateinit var recyclerView: RecyclerView
    private lateinit var saveButton: MaterialButton
    private lateinit var addTimeRangeButton: FloatingActionButton
    private lateinit var dayChipGroup: ChipGroup
    private lateinit var weekdaysChip: Chip
    private lateinit var weekendsChip: Chip
    private lateinit var timeRangeAdapter: TimeRangeAdapter
    private lateinit var timeRanges: MutableList<TimeRange>
    private val selectedDays = mutableSetOf<Int>()
    private val prefs by lazy {
        getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_AppBlocker)
        setContentView(R.layout.activity_schedule_settings)

        initViews()
        setupRecyclerView()
        loadSelectedDays()
        setupClickListeners()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        cancelButton = findViewById(R.id.cancelButton)
        statusChip = findViewById(R.id.statusChip)
        recyclerView = findViewById(R.id.timeRangeRecyclerView)
        saveButton = findViewById(R.id.saveScheduleButton)
        addTimeRangeButton = findViewById(R.id.addTimeButton)
        dayChipGroup = findViewById(R.id.dayChipGroup)
        weekdaysChip = findViewById(R.id.weekdaysChip)
        weekendsChip = findViewById(R.id.weekendsChip)
    }

    private fun setupRecyclerView() {
        timeRanges = loadTimeRanges().toMutableList()
        timeRangeAdapter = TimeRangeAdapter(
            timeRanges,
            onRemove = { position ->
                timeRanges.removeAt(position)
                timeRangeAdapter.notifyItemRemoved(position)
            },
            onTimeClick = { position, isStart ->
                showTimePicker(position, isStart)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = timeRangeAdapter
    }

    private fun loadSelectedDays() {
        val savedDays = prefs.getStringSet("schedule_days_of_week", emptySet())?.mapNotNull {
            it.toIntOrNull()
        }?.toSet() ?: emptySet()
        selectedDays.clear()
        selectedDays.addAll(savedDays)
        for (i in 0 until dayChipGroup.childCount) {
            val chip = dayChipGroup.getChildAt(i) as Chip
            val day = when (chip.text.toString()) {
                "Sun" -> Calendar.SUNDAY
                "Mon" -> Calendar.MONDAY
                "Tue" -> Calendar.TUESDAY
                "Wed" -> Calendar.WEDNESDAY
                "Thu" -> Calendar.THURSDAY
                "Fri" -> Calendar.FRIDAY
                "Sat" -> Calendar.SATURDAY
                else -> 0
            }
            chip.isChecked = selectedDays.contains(day)
        }
        updateStatusChip()
    }

    private fun setupClickListeners() {
        for (i in 0 until dayChipGroup.childCount) {
            val chip = dayChipGroup.getChildAt(i) as Chip
            chip.setOnCheckedChangeListener { _, isChecked ->
                val day = when (chip.text.toString()) {
                    "Sun" -> Calendar.SUNDAY
                    "Mon" -> Calendar.MONDAY
                    "Tue" -> Calendar.TUESDAY
                    "Wed" -> Calendar.WEDNESDAY
                    "Thu" -> Calendar.THURSDAY
                    "Fri" -> Calendar.FRIDAY
                    "Sat" -> Calendar.SATURDAY
                    else -> 0
                }
                if (isChecked) selectedDays.add(day) else selectedDays.remove(day)
                updateStatusChip()
            }
        }

        weekdaysChip.setOnClickListener {
            val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
            val allWeekdaysSelected = weekdays.all { day ->
                dayChipGroup.findViewWithTag<Chip>(day).isChecked
            }
            val newState = !allWeekdaysSelected
            weekdays.forEach { day ->
                dayChipGroup.findViewWithTag<Chip>(day).isChecked = newState
            }
            if (newState) {
                selectedDays.addAll(listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY))
            } else {
                selectedDays.removeAll(listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY))
            }
            updateStatusChip()
        }

        weekendsChip.setOnClickListener {
            val weekends = listOf("Sat", "Sun")
            val allWeekendsSelected = weekends.all { day ->
                dayChipGroup.findViewWithTag<Chip>(day).isChecked
            }
            val newState = !allWeekendsSelected
            weekends.forEach { day ->
                dayChipGroup.findViewWithTag<Chip>(day).isChecked = newState
            }
            if (newState) {
                selectedDays.addAll(listOf(Calendar.SATURDAY, Calendar.SUNDAY))
            } else {
                selectedDays.removeAll(listOf(Calendar.SATURDAY, Calendar.SUNDAY))
            }
            updateStatusChip()
        }

        addTimeRangeButton.setOnClickListener {
            timeRanges.add(TimeRange())
            timeRangeAdapter.notifyItemInserted(timeRanges.lastIndex)
        }

        saveButton.setOnClickListener {
            saveSchedule()
        }

        backButton.setOnClickListener {
            finish()
        }

        cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun updateStatusChip() {
        val count = selectedDays.size
        statusChip.text = when (count) {
            0 -> "0 selected"
            1 -> "1 selected"
            else -> "$count selected"
        }
    }

    private fun saveSchedule() {
        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "Please select at least one day", Toast.LENGTH_SHORT).show()
            return
        }
        val validRanges = timeRanges.filter { it.isValid() }
        if (validRanges.isEmpty()) {
            Toast.makeText(this, "Please add at least one valid time range", Toast.LENGTH_SHORT).show()
            return
        }
        prefs.edit().apply {
            putStringSet("schedule_days_of_week", selectedDays.map { it.toString() }.toSet())
            putString("time_ranges", TimeRangeStorage.serialize(validRanges))
            apply()
        }
        Toast.makeText(this, "Schedule saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onStart() {
        super.onStart()
        if (PinUtils.isPinSetup(this)) {
            PinLockActivity.launch(this, this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PinLockActivity.PIN_REQUEST_CODE && resultCode != RESULT_OK) {
            finish()
        }
    }

    private fun showTimePicker(index: Int, isStart: Boolean) {
        val range = timeRanges[index]
        val hour = if (isStart) range.startHour else range.endHour
        val minute = if (isStart) range.startMinute else range.endMinute
        TimePickerDialog(this, { _, h, m ->
            if (isStart) {
                timeRanges[index].startHour = h
                timeRanges[index].startMinute = m
            } else {
                timeRanges[index].endHour = h
                timeRanges[index].endMinute = m
            }
            timeRangeAdapter.notifyItemChanged(index)
        }, hour, minute, true).show()
    }

    private fun loadTimeRanges(): List<TimeRange> {
        val saved = prefs.getString("time_ranges", null)
        return if (saved != null) TimeRangeStorage.deserialize(saved) else listOf(TimeRange())
    }
}