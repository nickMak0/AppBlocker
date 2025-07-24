// File: app/src/main/java/com/example/appblocker/AppBlockerAccessibilityService.kt
package com.example.appblocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AppBlockerAccessibilityService : AccessibilityService() {

    private val blockedApps: Set<String>
        get() {
            val prefs = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
            return prefs.getStringSet("blockedApps", emptySet()) ?: emptySet()
        }

    private var lastBlockedPackage: String? = null
    private var lastBlockTime: Long = 0L
    private val blockInterval = 1500L // in milliseconds

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName == null) return
        val prefs = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
        val blockingEnabled = prefs.getBoolean("blockingEnabled", true)

        if (!blockingEnabled) {
            Log.d("ACCESS_SERVICE", "Blocking disabled by toggle.")
            return
        }
        val currentPackage = event.packageName.toString()
        Log.d("ACCESS_SERVICE", "Detected app: $currentPackage")

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
}
