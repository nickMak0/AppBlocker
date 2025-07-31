// File: app/src/main/java/com/example/appblocker/AppBlockerAccessibilityService.kt
package com.example.appblocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.appblocker.model.TimeRange
import com.example.appblocker.utils.AppScheduleChecker
import com.example.appblocker.utils.StatsManager         // ✅ Tracks app block count
import com.example.appblocker.utils.TimeRangeStorage
import com.example.appblocker.utils.TimeUtils
import java.util.*

class AppBlockerAccessibilityService : AccessibilityService() {

    private val blockInterval = 1000L // Reduced from 1500ms to 1000ms for faster response
    private var lastBlockedPackage: String? = null
    private var lastBlockTime: Long = 0L
    
    // Cache preferences for better performance
    private var cachedBlockedApps: Set<String> = emptySet()
    private var cachedBlockingEnabled: Boolean = true
    private var lastPrefsUpdate: Long = 0L
    private val prefsCacheTimeout = 5000L // 5 seconds cache

    private fun updateCachedPreferences() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPrefsUpdate > prefsCacheTimeout) {
            val prefs = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
            cachedBlockedApps = prefs.getStringSet("blockedApps", emptySet()) ?: emptySet()
            cachedBlockingEnabled = prefs.getBoolean("blocking_enabled", true)
            lastPrefsUpdate = currentTime
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName == null) return

        // Update cached preferences
        updateCachedPreferences()

        if (!cachedBlockingEnabled) {
            return
        }

        val currentPackage = event.packageName.toString()

        if (cachedBlockedApps.contains(currentPackage)) {
            // Check individual app schedule first, then fall back to global schedule
            val hasIndividualSchedule = AppScheduleChecker.hasAppSchedule(this, currentPackage)
            val shouldBlock = if (hasIndividualSchedule) {
                // Use individual app schedule
                AppScheduleChecker.isAppBlockedBySchedule(this, currentPackage)
            } else {
                // Fall back to global schedule
                isWithinScheduledTime()
            }
            
            if (!shouldBlock) {
                return
            }
            val currentTime = System.currentTimeMillis()

            // Avoid rapid repeat blocking
            if (currentPackage == lastBlockedPackage && currentTime - lastBlockTime < blockInterval) {
                return
            }

            lastBlockedPackage = currentPackage
            lastBlockTime = currentTime

            Log.d("ACCESS_SERVICE", "Blocking app: $currentPackage")

            // ✅ Update daily stat
            StatsManager.incrementAppsBlocked(this)

            // Launch block screen immediately
            val blockIntent = Intent(this, BlockScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
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
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or 
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                        AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 25 // Further reduced for ultra-fast detection
        }

        Log.d("ACCESS_SERVICE", "AppBlocker Accessibility Service connected.")
    }

    private fun isWithinScheduledTime(): Boolean {
        val prefs = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)

        val timeRangesJson = prefs.getString("time_ranges", null)
        val savedDaysSet = prefs.getStringSet("schedule_days_of_week", null)

        if (savedDaysSet.isNullOrEmpty()) {
            return false
        }

        val selectedDays = savedDaysSet.mapNotNull { it.toIntOrNull() }.toSet()
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        if (!selectedDays.contains(currentDay)) {
            return false
        }

        val timeRanges: List<TimeRange> = try {
            timeRangesJson?.let { TimeRangeStorage.deserialize(it) } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        return TimeUtils.isInAnyRange(timeRanges)
    }
}
