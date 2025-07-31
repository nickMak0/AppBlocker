package com.example.appblocker.utils

import android.content.Context
import com.example.appblocker.model.AppSchedule
import com.example.appblocker.model.TimeRange
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object AppScheduleStorage {
    private const val PREF_KEY = "app_schedules"
    private val gson = Gson()

    fun saveAppSchedule(context: Context, schedule: AppSchedule) {
        val prefs = context.getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
        val schedules = getAllSchedules(context).toMutableMap()
        schedules[schedule.packageName] = schedule
        
        val json = gson.toJson(schedules)
        prefs.edit().putString(PREF_KEY, json).apply()
    }

    fun getAppSchedule(context: Context, packageName: String): AppSchedule? {
        return getAllSchedules(context)[packageName]
    }

    fun getAllSchedules(context: Context): Map<String, AppSchedule> {
        val prefs = context.getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
        val json = prefs.getString(PREF_KEY, null) ?: return emptyMap()
        
        return try {
            val type = object : TypeToken<Map<String, AppSchedule>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun removeAppSchedule(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
        val schedules = getAllSchedules(context).toMutableMap()
        schedules.remove(packageName)
        
        val json = gson.toJson(schedules)
        prefs.edit().putString(PREF_KEY, json).apply()
    }
}