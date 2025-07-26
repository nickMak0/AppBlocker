// File: app/src/main/java/com/example/appblocker/dialogs/TimeRangeDialog.kt
package com.example.appblocker.dialogs

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.appblocker.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import java.util.*

class TimeRangeDialog(
    private val initialStartHour: Int = 9,
    private val initialStartMinute: Int = 0,
    private val initialEndHour: Int = 17,
    private val initialEndMinute: Int = 0,
    private val onTimeRangeSelected: (startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) -> Unit
) : DialogFragment() {

    private var startHour = initialStartHour
    private var startMinute = initialStartMinute
    private var endHour = initialEndHour
    private var endMinute = initialEndMinute

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view: View = LayoutInflater.from(context).inflate(R.layout.dialog_add_time_range, null)

        val startTimeButton = view.findViewById<MaterialButton>(R.id.startTimeButton)
        val endTimeButton = view.findViewById<MaterialButton>(R.id.endTimeButton)
        val durationChip = view.findViewById<Chip>(R.id.durationChip)
        val cancelButton = view.findViewById<MaterialButton>(R.id.cancelButton)
        val addButton = view.findViewById<MaterialButton>(R.id.addButton)

        startTimeButton.text = formatTime(startHour, startMinute)
        endTimeButton.text = formatTime(endHour, endMinute)
        durationChip.text = calculateDurationLabel(startHour, startMinute, endHour, endMinute)

        startTimeButton.setOnClickListener {
            TimePickerDialog(requireContext(), { _, hour, minute ->
                startHour = hour
                startMinute = minute
                startTimeButton.text = formatTime(hour, minute)
                durationChip.text = calculateDurationLabel(startHour, startMinute, endHour, endMinute)
            }, startHour, startMinute, false).show()
        }

        endTimeButton.setOnClickListener {
            TimePickerDialog(requireContext(), { _, hour, minute ->
                endHour = hour
                endMinute = minute
                endTimeButton.text = formatTime(hour, minute)
                durationChip.text = calculateDurationLabel(startHour, startMinute, endHour, endMinute)
            }, endHour, endMinute, false).show()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        addButton.setOnClickListener {
            onTimeRangeSelected(startHour, startMinute, endHour, endMinute)
            dialog.dismiss()
        }

        return dialog
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val is24Hour = false
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)

        return android.text.format.DateFormat.format(if (is24Hour) "HH:mm" else "hh:mm a", calendar).toString()
    }

    private fun calculateDurationLabel(startH: Int, startM: Int, endH: Int, endM: Int): String {
        val start = startH * 60 + startM
        val end = endH * 60 + endM
        val duration = if (end >= start) end - start else (24 * 60 - start + end)
        val hours = duration / 60
        val minutes = duration % 60

        return if (minutes == 0)
            "Duration: $hours hour${if (hours == 1) "" else "s"}"
        else
            "Duration: ${hours}h ${minutes}m"
    }
}
