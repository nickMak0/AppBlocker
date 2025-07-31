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
        val durationText: TextView = view.findViewById(R.id.durationText)

        fun bind(position: Int) {
            val range = timeRanges[position]
            startTime.text = String.format("%02d:%02d", range.startHour, range.startMinute)
            endTime.text = String.format("%02d:%02d", range.endHour, range.endMinute)
            
            // Calculate and display dynamic duration
            val duration = calculateDuration(range)
            durationText.text = duration

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
        
        private fun calculateDuration(range: TimeRange): String {
            val startMinutes = range.startHour * 60 + range.startMinute
            val endMinutes = range.endHour * 60 + range.endMinute
            
            val durationMinutes = if (endMinutes > startMinutes) {
                endMinutes - startMinutes
            } else {
                // Handle overnight duration
                (24 * 60) - startMinutes + endMinutes
            }
            
            val hours = durationMinutes / 60
            val minutes = durationMinutes % 60
            
            return when {
                hours == 0 -> "${minutes}m duration"
                minutes == 0 -> "${hours}h duration"
                else -> "${hours}h ${minutes}m duration"
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
