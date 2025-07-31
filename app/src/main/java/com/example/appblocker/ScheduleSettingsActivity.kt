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
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class ScheduleSettingsActivity : AppCompatActivity() {

    private lateinit var cancelButton: MaterialButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var saveButton: MaterialButton
    private lateinit var addTimeRangeButton: FloatingActionButton
    private lateinit var scheduleEnabledSwitch: SwitchMaterial
    private lateinit var weekdaysCard: MaterialCardView
    private lateinit var weekendsCard: MaterialCardView
    private lateinit var timeRangeAdapter: TimeRangeAdapter
    private lateinit var timeRanges: MutableList<TimeRange>
    private val selectedDays = mutableSetOf<Int>()
    private val dayCards = mutableMapOf<Int, MaterialCardView>()
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
        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        
        cancelButton = findViewById(R.id.cancelButton)
        recyclerView = findViewById(R.id.timeRangeRecyclerView)
        saveButton = findViewById(R.id.saveScheduleButton)
        addTimeRangeButton = findViewById(R.id.addTimeButton)
        scheduleEnabledSwitch = findViewById(R.id.scheduleEnabledSwitch)
        weekdaysCard = findViewById(R.id.weekdaysCard)
        weekendsCard = findViewById(R.id.weekendsCard)
        
        // Initialize day cards
        dayCards[Calendar.SUNDAY] = findViewById(R.id.sundayCard)
        dayCards[Calendar.MONDAY] = findViewById(R.id.mondayCard)
        dayCards[Calendar.TUESDAY] = findViewById(R.id.tuesdayCard)
        dayCards[Calendar.WEDNESDAY] = findViewById(R.id.wednesdayCard)
        dayCards[Calendar.THURSDAY] = findViewById(R.id.thursdayCard)
        dayCards[Calendar.FRIDAY] = findViewById(R.id.fridayCard)
        dayCards[Calendar.SATURDAY] = findViewById(R.id.saturdayCard)
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
        
        // Update day card appearances
        dayCards.forEach { (day, card) ->
            updateDayCardAppearance(card, selectedDays.contains(day))
        }
        
        // Load schedule enabled state
        scheduleEnabledSwitch.isChecked = prefs.getBoolean("schedule_enabled", false)
    }
    
    private fun updateDayCardAppearance(card: MaterialCardView, isSelected: Boolean) {
        if (isSelected) {
            card.setCardBackgroundColor(getColor(R.color.primary_color))
            card.strokeColor = getColor(R.color.primary_color)
        } else {
            card.setCardBackgroundColor(getColor(R.color.white))
            card.strokeColor = getColor(R.color.light_gray)
        }
    }

    private fun setupClickListeners() {
        // Day card click listeners
        dayCards.forEach { (day, card) ->
            card.setOnClickListener {
                val isSelected = selectedDays.contains(day)
                if (isSelected) {
                    selectedDays.remove(day)
                } else {
                    selectedDays.add(day)
                }
                updateDayCardAppearance(card, !isSelected)
            }
        }

        weekdaysCard.setOnClickListener {
            val weekdays = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
            val allWeekdaysSelected = weekdays.all { selectedDays.contains(it) }
            val newState = !allWeekdaysSelected
            
            if (newState) {
                selectedDays.addAll(weekdays)
            } else {
                selectedDays.removeAll(weekdays.toSet())
            }
            
            weekdays.forEach { day ->
                dayCards[day]?.let { card -> updateDayCardAppearance(card, newState) }
            }
        }

        weekendsCard.setOnClickListener {
            val weekends = listOf(Calendar.SATURDAY, Calendar.SUNDAY)
            val allWeekendsSelected = weekends.all { selectedDays.contains(it) }
            val newState = !allWeekendsSelected
            
            if (newState) {
                selectedDays.addAll(weekends)
            } else {
                selectedDays.removeAll(weekends.toSet())
            }
            
            weekends.forEach { day ->
                dayCards[day]?.let { card -> updateDayCardAppearance(card, newState) }
            }
        }

        addTimeRangeButton.setOnClickListener {
            timeRanges.add(TimeRange())
            timeRangeAdapter.notifyItemInserted(timeRanges.lastIndex)
        }
        
        // Also handle empty state add button
        findViewById<MaterialButton>(R.id.emptyStateAddButton)?.setOnClickListener {
            timeRanges.add(TimeRange())
            timeRangeAdapter.notifyItemInserted(timeRanges.lastIndex)
        }

        saveButton.setOnClickListener {
            saveSchedule()
        }

        cancelButton.setOnClickListener {
            finish()
        }
        
        scheduleEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("schedule_enabled", isChecked).apply()
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
            putBoolean("schedule_enabled", scheduleEnabledSwitch.isChecked)
            apply()
        }
        Toast.makeText(this, "Schedule saved successfully", Toast.LENGTH_SHORT).show()
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