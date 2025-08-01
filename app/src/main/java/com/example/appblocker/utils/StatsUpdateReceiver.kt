package com.example.appblocker.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StatsUpdateReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_STATS_UPDATED = "com.example.appblocker.STATS_UPDATED"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        // This will be handled by activities that register for this broadcast
    }
}