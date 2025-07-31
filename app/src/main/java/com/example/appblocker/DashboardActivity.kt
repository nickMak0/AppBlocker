// File: app/src/main/java/com/example/appblocker/DashboardActivity.kt
package com.example.appblocker

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appblocker.adapters.UsageStatsAdapter
import com.example.appblocker.databinding.ActivityDashboardBinding
import com.example.appblocker.model.UsageStatItem
import com.example.appblocker.utils.StatsManager
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var usageStatsAdapter: UsageStatsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupButtons()
        updateDateText()

        if (!hasUsageAccessPermission()) {
            Toast.makeText(this, "Please grant Usage Access permission", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else {
            loadLiveStats()
        }
    }

    override fun onResume() {
        super.onResume()
        loadLiveStats()
    }

    private fun setupRecyclerView() {
        usageStatsAdapter = UsageStatsAdapter(emptyList(), packageManager)
        binding.usageStatsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.usageStatsRecyclerView.adapter = usageStatsAdapter
    }

    private fun setupButtons() {
        binding.manageAppsButton.setOnClickListener {
            startActivity(Intent(this, ManageAppsActivity::class.java))
        }

        binding.scheduleButton.setOnClickListener {
            startActivity(Intent(this, ScheduleSettingsActivity::class.java))
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun updateDateText() {
        // Date text removed from redesigned dashboard
    }

    private fun loadLiveStats() {
        try {
            val appsBlocked = StatsManager.getAppsBlocked(this)
            val sitesBlocked = StatsManager.getSitesBlocked(this)
            // Streak removed

            // Updated references to direct binding elements
            binding.appsBlockedCount.text = appsBlocked.toString()
            binding.sitesBlockedCount.text = sitesBlocked.toString()
            // Focus time removed - redundant metric
            // Streak removed - not meaningful for app blocker

            updateScreenTime()

        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error loading live stats", e)
            showDefaultStatsFallback()
        }
    }

    private fun updateScreenTime() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        var totalScreenTime = 0L
        var foregroundAppStartTime: Long? = null
        val event = android.app.usage.UsageEvents.Event()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foregroundAppStartTime = event.timeStamp
            } else if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND && foregroundAppStartTime != null) {
                totalScreenTime += event.timeStamp - foregroundAppStartTime
                foregroundAppStartTime = null
            }
        }

        // Screen time removed

        loadUsageStats(startTime, endTime)
    }

    private fun loadUsageStats(startTime: Long, endTime: Long) {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )

        val usageMap = usageStats.associateBy { it.packageName }
        val launchableApps = getLaunchableApps()

        val usageItems = launchableApps.map { app ->
            val minutesUsed = usageMap[app.packageName]?.let {
                TimeUnit.MILLISECONDS.toMinutes(it.totalTimeInForeground)
            } ?: 0L

            val appName = try {
                app.loadLabel(packageManager).toString()
            } catch (e: Exception) {
                app.packageName
            }

            UsageStatItem(app.packageName, appName, minutesUsed)
        }.sortedByDescending { it.minutesUsed }.take(15)

        usageStatsAdapter.updateData(usageItems)
    }

    private fun getLaunchableApps(): List<ApplicationInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val launchables = packageManager.queryIntentActivities(intent, 0)
        return launchables.map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != this.packageName }
            .sortedBy {
                try {
                    it.loadLabel(packageManager).toString().lowercase()
                } catch (e: Exception) {
                    it.packageName
                }
            }
    }

    private fun hasUsageAccessPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun formatMinutesToHrsMins(minutes: Long): String {
        val hours = minutes / 60
        val remaining = minutes % 60
        return if (hours > 0) "${hours}h ${remaining}m" else "${remaining}m"
    }

    // Removed getBlockedAppsCount - no longer needed

    private fun showDefaultStatsFallback() {
        binding.appsBlockedCount.text = "0"
        binding.sitesBlockedCount.text = "0"
        // Focus time removed
        // Streak removed
    }
}
