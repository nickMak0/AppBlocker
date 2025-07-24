// File: app/src/main/java/com/example/appblocker/AppBlockerAccessibilityService.kt
package com.example.appblocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.appblocker.model.TimeRange
import com.example.appblocker.utils.TimeRangeStorage
import com.example.appblocker.utils.TimeUtils
import java.util.*

class AppBlockerAccessibilityService : AccessibilityService() {

    private val blockInterval = 1500L // in milliseconds
    private var lastBlockedPackage: String? = null
    private var lastBlockTime: Long = 0L

    private val blockedApps: Set<String>
        get() {
            val prefs = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
            return prefs.getStringSet("blockedApps", emptySet()) ?: emptySet()
        }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName == null) return

        val prefs = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
        val blockingEnabled = prefs.getBoolean("blocking_enabled", true)
        if (!blockingEnabled) {
            Log.d("ACCESS_SERVICE", "Blocking is disabled by toggle.")
            return
        }

        if (!isWithinScheduledTime()) {
            Log.d("ACCESS_SERVICE", "Current time is outside scheduled block range.")
            return
        }

        val currentPackage = event.packageName.toString()
        Log.d("ACCESS_SERVICE", "Detected package: $currentPackage")

        if (blockedApps.contains(currentPackage)) {
            val currentTime = System.currentTimeMillis()

            if (currentPackage == lastBlockedPackage && currentTime - lastBlockTime < blockInterval) {
                Log.d("ACCESS_SERVICE", "Duplicate block avoided for: $currentPackage")
                return
            }

            lastBlockedPackage = currentPackage
            lastBlockTime = currentTime

            Log.d("ACCESS_SERVICE", "Blocking app: $currentPackage")

            val blockIntent = Intent(this, BlockScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(blockIntent)
        }
    }

    override fun onInterrupt() {
        // Required override
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }

        Log.d("ACCESS_SERVICE", "Accessibility service connected")
    }

    private fun isWithinScheduledTime(): Boolean {
        val prefs = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)

        val timeRangesJson = prefs.getString("time_ranges", null)
        val savedDaysSet = prefs.getStringSet("schedule_days_of_week", null)

        if (savedDaysSet.isNullOrEmpty()) {
            Log.d("ACCESS_SERVICE", "No days selected — skipping block.")
            return false
        }

        val selectedDays = savedDaysSet.mapNotNull { it.toIntOrNull() }.toSet()
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        if (!selectedDays.contains(currentDay)) {
            Log.d("ACCESS_SERVICE", "Today ($currentDay) not in selected schedule days.")
            return false
        }

        val timeRanges: List<TimeRange> = try {
            timeRangesJson?.let { TimeRangeStorage.deserialize(it) } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        val result = TimeUtils.isInAnyRange(timeRanges)
        Log.d("ACCESS_SERVICE", "Time check result: $result")
        return result
    }

}
