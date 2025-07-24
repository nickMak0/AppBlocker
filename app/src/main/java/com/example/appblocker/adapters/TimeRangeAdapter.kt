// File: app/src/main/java/com/example/appblocker/adapters/TimeRangeAdapter.kt
package com.example.appblocker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appblocker.R
import com.example.appblocker.model.TimeRange

class TimeRangeAdapter(
    private val timeRanges: List<TimeRange>,
    private val onRemove: (Int) -> Unit,
    private val onTimeClick: (Int, Boolean) -> Unit // <-- new param for clock click
) : RecyclerView.Adapter<TimeRangeAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val startTime: TextView = view.findViewById(R.id.startTimeTextView)
        val endTime: TextView = view.findViewById(R.id.endTimeTextView)
        val removeButton: View = view.findViewById(R.id.removeTimeRangeButton)


        fun bind(position: Int) {
            val range = timeRanges[position]
            startTime.text = String.format("%02d:%02d", range.startHour, range.startMinute)
            endTime.text = String.format("%02d:%02d", range.endHour, range.endMinute)

            startTime.setOnClickListener {
                onTimeClick(position, true)
            }

            endTime.setOnClickListener {
                onTimeClick(position, false)
            }

            removeButton.setOnClickListener {
                onRemove(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_time_range, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = timeRanges.size
}
