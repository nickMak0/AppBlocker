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
import android.content.BroadcastReceiver
import android.content.IntentFilter
import java.util.*
import java.util.concurrent.TimeUnit

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var usageStatsAdapter: UsageStatsAdapter
    
    // Stats update receiver
    private val statsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            android.util.Log.d("DashboardActivity", "Stats update broadcast received")
            loadLiveStats()
        }
    }

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
    
    override fun onStart() {
        super.onStart()
        loadLiveStats()
        // Register for stats updates
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statsReceiver, IntentFilter("com.example.appblocker.STATS_UPDATED"), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(statsReceiver, IntentFilter("com.example.appblocker.STATS_UPDATED"))
        }
    }
    
    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(statsReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
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
        
        binding.viewAllUsageButton.setOnClickListener {
            startActivity(Intent(this, FullUsageActivity::class.java))
        }
        
        // Test functionality - click View All button 3 times quickly to test stats
        var clickCount = 0
        var lastClickTime = 0L
        binding.viewAllUsageButton.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < 1000) {
                clickCount++
                if (clickCount >= 2) {
                    StatsManager.testIncrementStats(this)
                    Toast.makeText(this, "Test stats incremented!", Toast.LENGTH_SHORT).show()
                    clickCount = 0
                }
            } else {
                clickCount = 0
                startActivity(Intent(this, FullUsageActivity::class.java))
            }
            lastClickTime = currentTime
        }
    }

    private fun updateDateText() {
        // Date text removed from redesigned dashboard
    }

    private fun loadLiveStats() {
        try {
            StatsManager.init(this)
            val appsBlocked = StatsManager.getAppsBlocked(this)
            val sitesBlocked = StatsManager.getSitesBlocked(this)

            android.util.Log.d("DashboardActivity", "Updating stats - Apps Blocked Today: $appsBlocked, Sites Blocked Today: $sitesBlocked")
            
            binding.appsBlockedCount.text = appsBlocked.toString()
            binding.sitesBlockedCount.text = sitesBlocked.toString()

            updateScreenTime()
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error loading stats", e)
            binding.appsBlockedCount.text = "0"
            binding.sitesBlockedCount.text = "0"
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

        usageStatsAdapter.updateData(usageItems.filter { it.minutesUsed > 0 }.take(10))
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


}
