package com.example.appblocker.adapters

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appblocker.R
import com.example.appblocker.model.UsageStatItem

class UsageStatsAdapter(
    private var items: List<UsageStatItem>,
    private val packageManager: PackageManager
) : RecyclerView.Adapter<UsageStatsAdapter.UsageStatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsageStatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_usage_stat, parent, false)
        return UsageStatViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsageStatViewHolder, position: Int) {
        val item = items[position]
        holder.appName.text = item.appName
        holder.usageTime.text = "${item.minutesUsed} min"

        try {
            val icon: Drawable = packageManager.getApplicationIcon(item.packageName)
            holder.appIcon.setImageDrawable(icon)
        } catch (e: PackageManager.NameNotFoundException) {
            holder.appIcon.setImageResource(R.drawable.ic_android_placeholder)
        }
    }

    override fun getItemCount(): Int = items.size

    // Add this method to update data
    fun updateData(newItems: List<UsageStatItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    class UsageStatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
        val appName: TextView = itemView.findViewById(R.id.appName)
        val usageTime: TextView = itemView.findViewById(R.id.usageTime)
    }
}