// File: app/src/main/java/com/example/appblocker/AppBlockerAccessibilityService.kt
package com.example.appblocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.appblocker.model.TimeRange
import com.example.appblocker.utils.StatsManager         // ✅ Tracks app block count
import com.example.appblocker.utils.TimeRangeStorage
import com.example.appblocker.utils.TimeUtils
import java.util.*

class AppBlockerAccessibilityService : AccessibilityService() {

    private val blockInterval = 1500L // ms between same-app blocks
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
            Log.d("ACCESS_SERVICE", "Blocking disabled by user toggle.")
            return
        }

        if (!isWithinScheduledTime()) {
            Log.d("ACCESS_SERVICE", "Outside blocking schedule.")
            return
        }

        val currentPackage = event.packageName.toString()
        Log.d("ACCESS_SERVICE", "Detected package: $currentPackage")

        if (blockedApps.contains(currentPackage)) {
            val currentTime = System.currentTimeMillis()

            // Avoid rapid repeat blocking
            if (currentPackage == lastBlockedPackage && currentTime - lastBlockTime < blockInterval) {
                Log.d("ACCESS_SERVICE", "Duplicate block avoided: $currentPackage")
                return
            }

            lastBlockedPackage = currentPackage
            lastBlockTime = currentTime

            Log.d("ACCESS_SERVICE", "Blocking app: $currentPackage")

            // ✅ Update daily stat
            StatsManager.incrementAppsBlocked(this)

            // Launch block screen
            val blockIntent = Intent(this, BlockScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(blockIntent)
        }
    }

    override fun onInterrupt() {
        // Required method — not used
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }

        Log.d("ACCESS_SERVICE", "AppBlocker Accessibility Service connected.")
    }

    private fun isWithinScheduledTime(): Boolean {
        val prefs = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)

        val timeRangesJson = prefs.getString("time_ranges", null)
        val savedDaysSet = prefs.getStringSet("schedule_days_of_week", null)

        if (savedDaysSet.isNullOrEmpty()) {
            Log.d("ACCESS_SERVICE", "No scheduled days set.")
            return false
        }

        val selectedDays = savedDaysSet.mapNotNull { it.toIntOrNull() }.toSet()
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        if (!selectedDays.contains(currentDay)) {
            Log.d("ACCESS_SERVICE", "Today ($currentDay) is not a scheduled block day.")
            return false
        }

        val timeRanges: List<TimeRange> = try {
            timeRangesJson?.let { TimeRangeStorage.deserialize(it) } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        val isWithin = TimeUtils.isInAnyRange(timeRanges)
        Log.d("ACCESS_SERVICE", "Within time range: $isWithin")
        return isWithin
    }
}
