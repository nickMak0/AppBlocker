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
    private val strictBlockInterval = 200L // Ultra-fast blocking in strict mode
    private var lastBlockedPackage: String? = null
    private var lastBlockTime: Long = 0L
    
    // Cache preferences for better performance
    private var cachedBlockedApps: Set<String> = emptySet()
    private var cachedBlockingEnabled: Boolean = true
    private var cachedStrictMode: Boolean = false
    private var lastPrefsUpdate: Long = 0L
    private val prefsCacheTimeout = 5000L // 5 seconds cache

    private fun updateCachedPreferences() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPrefsUpdate > prefsCacheTimeout) {
            val prefs = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
            cachedBlockedApps = prefs.getStringSet("blockedApps", emptySet()) ?: emptySet()
            cachedBlockingEnabled = prefs.getBoolean("blocking_enabled", true)
            cachedStrictMode = prefs.getBoolean("strict_mode_enabled", false)
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
        
        // Check if break mode is active
        val prefs = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
        val isBreakModeActive = prefs.getBoolean("break_mode_active", false)
        if (isBreakModeActive) {
            return
        }

        val currentPackage = event.packageName.toString()

        if (cachedBlockedApps.contains(currentPackage)) {
            // Check individual app schedule first, then fall back to global schedule
            val hasIndividualSchedule = AppScheduleChecker.hasAppSchedule(this, currentPackage)
            val shouldBlock = if (hasIndividualSchedule) {
                // Use individual app schedule
                val blocked = AppScheduleChecker.isAppBlockedBySchedule(this, currentPackage)
                Log.d("ACCESS_SERVICE", "App $currentPackage has individual schedule, blocked: $blocked")
                blocked
            } else {
                // Fall back to global schedule - if no global schedule, block always
                val prefs = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
                val isScheduleEnabled = prefs.getBoolean("schedule_enabled", false)
                val blocked = if (isScheduleEnabled) {
                    isWithinScheduledTime()
                } else {
                    true // No schedule means block always
                }
                Log.d("ACCESS_SERVICE", "App $currentPackage using global schedule, enabled: $isScheduleEnabled, blocked: $blocked")
                blocked
            }
            
            if (!shouldBlock) {
                return
            }
            val currentTime = System.currentTimeMillis()

            // Strict mode uses faster blocking interval
            val interval = if (cachedStrictMode) strictBlockInterval else blockInterval
            
            // Avoid rapid repeat blocking
            if (currentPackage == lastBlockedPackage && currentTime - lastBlockTime < interval) {
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
                putExtra("strict_mode", cachedStrictMode)
            }
            startActivity(blockIntent)
            
            // In strict mode, also try to force close the app
            if (cachedStrictMode) {
                try {
                    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                    activityManager.killBackgroundProcesses(currentPackage)
                } catch (e: Exception) {
                    Log.w("ACCESS_SERVICE", "Could not kill background process: ${e.message}")
                }
            }
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
            notificationTimeout = 10 // Ultra-fast detection for strict mode
        }

        Log.d("ACCESS_SERVICE", "AppBlocker Accessibility Service connected.")
    }

    private fun isWithinScheduledTime(): Boolean {
        val prefs = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)

        val timeRangesJson = prefs.getString("time_ranges", null)
        val savedDaysSet = prefs.getStringSet("schedule_days_of_week", null)

        // If no days selected, don't block based on schedule
        if (savedDaysSet.isNullOrEmpty()) {
            Log.d("ACCESS_SERVICE", "No days selected in global schedule")
            return false
        }

        val selectedDays = savedDaysSet.mapNotNull { it.toIntOrNull() }.toSet()
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        // If today is not in selected days, don't block
        if (!selectedDays.contains(currentDay)) {
            Log.d("ACCESS_SERVICE", "Current day $currentDay not in selected days $selectedDays")
            return false
        }

        val timeRanges: List<TimeRange> = try {
            timeRangesJson?.let { TimeRangeStorage.deserialize(it) } ?: emptyList()
        } catch (e: Exception) {
            Log.e("ACCESS_SERVICE", "Error deserializing time ranges", e)
            return false
        }

        // If no time ranges, don't block
        if (timeRanges.isEmpty()) {
            Log.d("ACCESS_SERVICE", "No time ranges in global schedule")
            return false
        }

        val isInRange = TimeUtils.isInAnyRange(timeRanges)
        Log.d("ACCESS_SERVICE", "Current time in global schedule range: $isInRange")
        return isInRange
    }
}
