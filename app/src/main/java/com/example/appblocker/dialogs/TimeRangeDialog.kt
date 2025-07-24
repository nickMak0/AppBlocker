package com.example.appblocker.dialogs

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.appblocker.R
import java.util.*

class TimeRangeDialog(
    private val initialStartHour: Int = 0,
    private val initialStartMinute: Int = 0,
    private val initialEndHour: Int = 0,
    private val initialEndMinute: Int = 0,
    private val onTimeRangeSelected: (startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) -> Unit
) : DialogFragment() {

    private var startHour = initialStartHour
    private var startMinute = initialStartMinute
    private var endHour = initialEndHour
    private var endMinute = initialEndMinute

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_time_range, null)
        val startTimeView = view.findViewById<TextView>(R.id.dialogStartTime)
        val endTimeView = view.findViewById<TextView>(R.id.dialogEndTime)

        startTimeView.text = formatTime(startHour, startMinute)
        endTimeView.text = formatTime(endHour, endMinute)

        startTimeView.setOnClickListener {
            TimePickerDialog(requireContext(), { _, hour, minute ->
                startHour = hour
                startMinute = minute
                startTimeView.text = formatTime(hour, minute)
            }, startHour, startMinute, true).show()
        }

        endTimeView.setOnClickListener {
            TimePickerDialog(requireContext(), { _, hour, minute ->
                endHour = hour
                endMinute = minute
                endTimeView.text = formatTime(hour, minute)
            }, endHour, endMinute, true).show()
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Select Time Range")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                onTimeRangeSelected(startHour, startMinute, endHour, endMinute)
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    private fun formatTime(hour: Int, minute: Int): String {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }
}
