package com.example.appblocker.utils

import android.content.Context
import java.util.concurrent.TimeUnit

object StatsManager {

    private const val PREFS_NAME = "AppBlockerStats"
    private const val KEY_APPS_BLOCKED = "apps_blocked_today"
    private const val KEY_SITES_BLOCKED = "sites_blocked_today"
    private const val KEY_FOCUS_TIME_MINUTES = "focus_time_minutes"
    private const val KEY_STREAK_DAYS = "focus_streak"
    private const val KEY_LAST_UPDATED_DATE = "last_updated_date"
    private const val KEY_FOCUS_TIME_TODAY_MINUTES = "focus_time_today_minutes"

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = getTodayDate()
        val lastDate = prefs.getString(KEY_LAST_UPDATED_DATE, "")

        android.util.Log.d("StatsManager", "Init - Today: $today, Last: '$lastDate'")
        
        val currentApps = prefs.getInt(KEY_APPS_BLOCKED, 0)
        val currentSites = prefs.getInt(KEY_SITES_BLOCKED, 0)
        android.util.Log.d("StatsManager", "Current stats before init - Apps: $currentApps, Sites: $currentSites")

        if (lastDate != today && !lastDate.isNullOrEmpty()) {
            // Reset daily stats only if we have a previous date and it's different
            android.util.Log.d("StatsManager", "Resetting daily stats for new day")
            prefs.edit()
                .putInt(KEY_APPS_BLOCKED, 0)
                .putInt(KEY_SITES_BLOCKED, 0)
                .putInt(KEY_FOCUS_TIME_MINUTES, 0)
                .putString(KEY_LAST_UPDATED_DATE, today)
                .apply()
        } else if (lastDate.isNullOrEmpty()) {
            // First time setup - just set the date without resetting
            android.util.Log.d("StatsManager", "First time setup - keeping existing stats")
            prefs.edit().putString(KEY_LAST_UPDATED_DATE, today).apply()
        } else {
            android.util.Log.d("StatsManager", "Same day - no reset needed")
        }
    }

    fun incrementAppsBlocked(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_APPS_BLOCKED, 0)
        val newCount = current + 1
        prefs.edit().putInt(KEY_APPS_BLOCKED, newCount).apply()
        
        android.util.Log.d("StatsManager", "Apps blocked incremented to: $newCount")
        
        // Broadcast stats update
        val intent = android.content.Intent("com.example.appblocker.STATS_UPDATED")
        context.sendBroadcast(intent)
    }

    fun incrementSitesBlocked(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_SITES_BLOCKED, 0)
        val newCount = current + 1
        prefs.edit().putInt(KEY_SITES_BLOCKED, newCount).apply()
        
        android.util.Log.d("StatsManager", "Sites blocked incremented to: $newCount")
        
        // Broadcast stats update
        val intent = android.content.Intent("com.example.appblocker.STATS_UPDATED")
        context.sendBroadcast(intent)
    }

    fun getAppsBlocked(context: Context): Int {
        val count = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_APPS_BLOCKED, 0)
        android.util.Log.d("StatsManager", "Getting apps blocked: $count")
        return count
    }

    fun getSitesBlocked(context: Context): Int {
        val count = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_SITES_BLOCKED, 0)
        android.util.Log.d("StatsManager", "Getting sites blocked: $count")
        return count
    }

    // Test method to manually increment stats
    fun testIncrementStats(context: Context) {
        android.util.Log.d("StatsManager", "Test increment called")
        
        // Direct increment without broadcast for testing
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentApps = prefs.getInt(KEY_APPS_BLOCKED, 0)
        val currentSites = prefs.getInt(KEY_SITES_BLOCKED, 0)
        
        android.util.Log.d("StatsManager", "Before increment - Apps: $currentApps, Sites: $currentSites")
        
        prefs.edit()
            .putInt(KEY_APPS_BLOCKED, currentApps + 1)
            .putInt(KEY_SITES_BLOCKED, currentSites + 1)
            .apply()
            
        val newApps = prefs.getInt(KEY_APPS_BLOCKED, 0)
        val newSites = prefs.getInt(KEY_SITES_BLOCKED, 0)
        android.util.Log.d("StatsManager", "After increment - Apps: $newApps, Sites: $newSites")
        
        // Send broadcast
        val intent = android.content.Intent("com.example.appblocker.STATS_UPDATED")
        context.sendBroadcast(intent)
        android.util.Log.d("StatsManager", "Broadcast sent")
    }
    
    private fun getTodayDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
    }
}