package com.example.appblocker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appblocker.R

class BlockedSitesAdapter(
    private var domains: MutableList<String>,
    private val onRemove: (String) -> Unit
) : RecyclerView.Adapter<BlockedSitesAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val domainText: TextView = view.findViewById(R.id.domainText)
        val removeButton: ImageView = view.findViewById(R.id.removeButton)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_blocked_site, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val domain = domains[position]
        holder.domainText.text = domain
        holder.removeButton.setOnClickListener {
            onRemove(domain)
        }
    }
    
    override fun getItemCount() = domains.size
    
    fun updateDomains(newDomains: List<String>) {
        domains.clear()
        domains.addAll(newDomains)
        notifyDataSetChanged()
    }
}