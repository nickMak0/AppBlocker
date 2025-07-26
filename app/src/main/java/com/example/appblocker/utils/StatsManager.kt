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

        if (lastDate != today) {
            // Reset daily stats
            prefs.edit()
                .putInt(KEY_APPS_BLOCKED, 0)
                .putInt(KEY_SITES_BLOCKED, 0)
                .putInt(KEY_FOCUS_TIME_MINUTES, 0)
                .putString(KEY_LAST_UPDATED_DATE, today)
                .apply()
        }
    }

    fun incrementAppsBlocked(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_APPS_BLOCKED, 0)
        prefs.edit().putInt(KEY_APPS_BLOCKED, current + 1).apply()
    }
    fun getFocusTime(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_FOCUS_TIME_TODAY_MINUTES, 0)
    }
    fun incrementSitesBlocked(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_SITES_BLOCKED, 0)
        prefs.edit().putInt(KEY_SITES_BLOCKED, current + 1).apply()
    }

    fun addFocusMinutes(context: Context, minutes: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val total = prefs.getInt(KEY_FOCUS_TIME_MINUTES, 0)
        prefs.edit().putInt(KEY_FOCUS_TIME_MINUTES, (total + minutes).toInt()).apply()
    }

    fun setStreakDays(context: Context, days: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_STREAK_DAYS, days).apply()
    }

    fun getAppsBlocked(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_APPS_BLOCKED, 0)
    }

    fun getSitesBlocked(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_SITES_BLOCKED, 0)
    }

    fun getFocusTimeFormatted(context: Context): String {
        val totalMinutes = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_FOCUS_TIME_MINUTES, 0)
        val hours = TimeUnit.MINUTES.toHours(totalMinutes.toLong())
        val minutes = totalMinutes - TimeUnit.HOURS.toMinutes(hours)
        return "${hours}h ${minutes}m"
    }

    fun getStreakDays(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_STREAK_DAYS, 0)
    }

    private fun getTodayDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
    }
}
