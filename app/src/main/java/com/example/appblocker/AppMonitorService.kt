package com.example.appblocker

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log

class AppMonitorService : Service() {

    private val handler = Handler()
    private val interval = 2000L // Check every 2 seconds

    private val blockedApps = listOf(
        "com.whatsapp",
        "org.telegram.messenger"
    )

    private var lastBlockedApp: String? = null

    private val monitorRunnable = object : Runnable {
        override fun run() {
            checkForegroundApp()
            handler.postDelayed(this, interval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        handler.post(monitorRunnable)
    }

    override fun onDestroy() {
        handler.removeCallbacks(monitorRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun checkForegroundApp() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 5000

        val events = usageStatsManager.queryEvents(beginTime, endTime)
        var lastApp: String? = null

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastApp = event.packageName
            }
        }

        if (lastApp != null && blockedApps.contains(lastApp)) {
            if (lastApp == lastBlockedApp) return
            lastBlockedApp = lastApp

            Log.d("MONITOR_SERVICE", "Blocking: $lastApp")
            val intent = Intent(this, BlockScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        }
    }
}
